/**
 * queue-load-test.js — Redis 대기열 지속 부하 테스트 (k6)
 *
 * 목적: 5000 동시 VU가 지속적으로 큐에 진입/대기/이탈을 반복하여
 *      Redis 대기열 시스템을 포화 상태로 유지. 테스트 중 프론트엔드에서
 *      직접 큐에 진입해 대기 순서(position > 1)를 확인할 수 있다.
 *
 * ────────────────────────────────────────────────────────────────────
 * 사전 준비
 * ────────────────────────────────────────────────────────────────────
 * 1. DB 유저 시딩 (미완료 시)
 *      mysql -u root -p0000 open_ticket < seed-users.sql
 *
 * 2. JWT 토큰 사전 생성 (서버 기동 불필요)
 *      npm install jsonwebtoken mysql2
 *      node generate-tokens.js
 *      # → tokens.csv 생성 확인: wc -l tokens.csv
 *
 * 3. 서버 기동
 *      ./gradlew bootRun
 *
 * ────────────────────────────────────────────────────────────────────
 * 실행 방법
 * ────────────────────────────────────────────────────────────────────
 *   # 권장 (macOS fd 제한 자동 처리)
 *   ./run-load-test.sh
 *   ./run-load-test.sh -e VU_COUNT=3
 *   ./run-load-test.sh -e VU_COUNT=100 -e SLOT_HOLD_SECONDS=10 -e DURATION=10m
 *
 *   # fd 제한이 이미 충분한 환경
 *   k6 run queue-load-test.js
 *
 * ────────────────────────────────────────────────────────────────────
 * 환경 변수
 * ────────────────────────────────────────────────────────────────────
 *   BASE_URL              = http://localhost:8080
 *   EVENT_ID              = 1
 *   VU_COUNT              = 5000   # 최대 동시 VU
 *   RAMP_UP               = 60s    # VU 램프업 시간
 *   DURATION              = 30m    # 지속 부하 시간
 *   SLOT_HOLD_SECONDS     = 5      # ALLOWED 상태에서 슬롯 점유 시간
 *   POLL_INTERVAL_SECONDS = 3      # WAITING 상태 폴링 기본 간격
 *   MAX_WAIT_SECONDS      = 600    # 대기 타임아웃
 *   MAX_ACTIVE_SLOTS      = 100    # 큐 최대 활성 슬롯 (application.yml: queue.max-active-per-event)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';

const BASE_URL              = __ENV.BASE_URL              || 'http://localhost:8080';
const EVENT_ID              = Number(__ENV.EVENT_ID              || 1);
const VU_COUNT              = Number(__ENV.VU_COUNT              || 5000);
const RAMP_UP               = __ENV.RAMP_UP               || '60s';
const DURATION              = __ENV.DURATION              || '30m';
const SLOT_HOLD_SECONDS     = Number(__ENV.SLOT_HOLD_SECONDS     || 5);
const POLL_INTERVAL_SECONDS = Number(__ENV.POLL_INTERVAL_SECONDS || 3);
const MAX_WAIT_SECONDS      = Number(__ENV.MAX_WAIT_SECONDS      || 600);
const MAX_ACTIVE_SLOTS      = Number(__ENV.MAX_ACTIVE_SLOTS      || 100);

// 폴링 지터: 0~2초 랜덤 추가로 동시 폴링 스파이크 방지
const POLL_JITTER_SECONDS = 2;

// 메트릭
const queueAllowedRate      = new Rate('queue_allowed_rate');
const queueWaitingRate      = new Rate('queue_waiting_rate');
const queuePromotedRate     = new Rate('queue_promoted_rate');
const queueLeaveSuccessRate = new Rate('queue_leave_success_rate');
const queueEnterFailureCount  = new Counter('queue_enter_failure_count');
const queueStuckTimeoutCount  = new Counter('queue_stuck_timeout_count');
const queueLeaveFailureCount  = new Counter('queue_leave_failure_count');
const queueWaitSeconds        = new Trend('queue_wait_seconds');

// SharedArray: init 단계에서 1회 로드, 모든 VU가 공유
const tokens = new SharedArray('authTokens', function () {
  return open('./tokens.csv')
    .trim()
    .split('\n')
    .slice(1)                         // 헤더(userId,email,token) 제거
    .map(line => line.split(',')[2]); // token 컬럼 추출
});

export const options = {
  scenarios: {
    sustained_queue_load: {
      executor: 'ramping-vus',
      stages: [
        { duration: RAMP_UP,  target: VU_COUNT }, // 점진적 증가
        { duration: DURATION, target: VU_COUNT }, // 지속 유지
        { duration: '30s',    target: 0         }, // 점진적 감소
      ],
    },
  },
  thresholds: {
    http_req_failed:   ['rate<0.02'],
    http_req_duration: ['p(95)<2000'],
    queue_wait_seconds: ['p(90)<120'],
  },
};

// ─────────────────────────────────────────────
// setup: 토큰 수 사전 검증
// ─────────────────────────────────────────────

export function setup() {
  if (tokens.length < VU_COUNT) {
    throw new Error(
      `[ABORT] tokens.csv 고유 유저 부족: ${tokens.length}개 < VU_COUNT ${VU_COUNT}\n` +
      `  서버 userId 중복 제거로 실제 WAITING ≈ ${Math.max(0, tokens.length - MAX_ACTIVE_SLOTS)}\n` +
      `  해결: node generate-tokens.js (seed-users.sql 시딩 후 재실행)`
    );
  }
  console.log(`[OK] tokens: ${tokens.length}개 / VU_COUNT: ${VU_COUNT}`);
}

// ─────────────────────────────────────────────
// teardown: 테스트 종료 시 잔여 큐 항목 일괄 정리
// (Ctrl+C 포함 graceful stop 시 자동 실행)
// ─────────────────────────────────────────────

export function teardown() {
  const vuTokens = tokens.slice(0, Math.min(VU_COUNT, tokens.length));
  const CHUNK = 100; // 서버 과부하 방지를 위한 배치 크기
  let cleaned = 0;

  for (let i = 0; i < vuTokens.length; i += CHUNK) {
    const chunk = vuTokens.slice(i, i + CHUNK);

    // 1. enterQueue (idempotent): 큐에 있으면 기존 token 반환, 없으면 신규 생성
    const enterResps = http.batch(
      chunk.map(authToken => [
        'POST',
        `${BASE_URL}/api/v1/queue/events/${EVENT_ID}`,
        null,
        { headers: { Authorization: `Bearer ${authToken}` } },
      ])
    );

    // 2. leaveQueue: enterQueue에서 받은 token으로 즉시 이탈
    const leaveReqs = [];
    for (let j = 0; j < enterResps.length; j++) {
      let body;
      try { body = enterResps[j].json(); } catch (_) { continue; }
      if (!body || !body.data || !body.data.token) continue;
      leaveReqs.push([
        'POST',
        `${BASE_URL}/api/v1/queue/events/${EVENT_ID}/leave`,
        JSON.stringify({ queueToken: body.data.token }),
        {
          headers: {
            Authorization: `Bearer ${chunk[j]}`,
            'Content-Type': 'application/json',
          },
        },
      ]);
    }

    if (leaveReqs.length > 0) {
      http.batch(leaveReqs);
      cleaned += leaveReqs.length;
    }
  }

  console.log(`[teardown] 큐 정리 완료: ${cleaned}/${vuTokens.length}개 이탈`);
}

// ─────────────────────────────────────────────
// 헬퍼 함수
// ─────────────────────────────────────────────

function parseApiResponse(res, apiName) {
  let body;
  try {
    body = res.json();
  } catch (e) {
    console.error(`${apiName}: JSON 파싱 실패 - ${e}`);
    return null;
  }

  if (!body || typeof body !== 'object' || !('data' in body)) {
    console.error(`${apiName}: 응답 포맷이 예상과 다릅니다.`);
    return null;
  }

  return body.data;
}

function enterQueue(authToken) {
  const res = http.post(`${BASE_URL}/api/v1/queue/events/${EVENT_ID}`, null, {
    headers: { Authorization: `Bearer ${authToken}` },
  });

  const ok = check(res, { 'enterQueue status is 200': (r) => r.status === 200 });
  if (!ok) {
    queueEnterFailureCount.add(1);
    return null;
  }

  return parseApiResponse(res, 'enterQueue');
}

function checkQueueStatus(authToken, queueToken) {
  const encodedToken = encodeURIComponent(queueToken);
  const res = http.get(
    `${BASE_URL}/api/v1/queue/events/${EVENT_ID}?token=${encodedToken}`,
    { headers: { Authorization: `Bearer ${authToken}` } }
  );

  check(res, { 'checkStatus status is 200': (r) => r.status === 200 });

  return parseApiResponse(res, 'checkQueueStatus');
}

function leaveQueue(authToken, queueToken) {
  const res = http.post(
    `${BASE_URL}/api/v1/queue/events/${EVENT_ID}/leave`,
    JSON.stringify({ queueToken }),
    {
      headers: {
        Authorization: `Bearer ${authToken}`,
        'Content-Type': 'application/json',
      },
    }
  );

  const ok = check(res, { 'leaveQueue status is 200': (r) => r.status === 200 });
  if (!ok) {
    queueLeaveFailureCount.add(1);
    queueLeaveSuccessRate.add(false);
    return { ok: false, removed: false };
  }

  const data = parseApiResponse(res, 'leaveQueue');
  const removed = Boolean(data?.removed);
  queueLeaveSuccessRate.add(removed);
  if (!removed) {
    queueLeaveFailureCount.add(1);
  }
  return { ok: true, removed };
}

// ─────────────────────────────────────────────
// default: VU 루프 (진입→대기→점유→이탈→반복)
// ─────────────────────────────────────────────

export default function () {
  // SharedArray에서 순환 선택
  const authToken = tokens[(__VU - 1) % tokens.length];

  if (!authToken) {
    sleep(POLL_INTERVAL_SECONDS);
    return;
  }

  // eslint-disable-next-line no-constant-condition
  while (true) {
    // 1. 큐 진입
    const queueStatus = enterQueue(authToken);
    if (!queueStatus || !queueStatus.token) {
      sleep(POLL_INTERVAL_SECONDS);
      continue;
    }

    let current = queueStatus;
    const startedAsWaiting  = current.phase === 'WAITING';
    const waitingStartedAt  = Date.now();

    queueWaitingRate.add(current.phase === 'WAITING');

    // 2. WAITING 상태: 폴링 (지터 추가로 동시 스파이크 방지)
    while (current.phase === 'WAITING') {
      const interval = current.pollIntervalSeconds ?? POLL_INTERVAL_SECONDS;
      sleep(interval + Math.random() * POLL_JITTER_SECONDS);

      const elapsedSeconds = (Date.now() - waitingStartedAt) / 1000;
      if (elapsedSeconds > MAX_WAIT_SECONDS) {
        queueStuckTimeoutCount.add(1);
        queuePromotedRate.add(false);
        // 타임아웃 시 이탈 시도 후 루프 재시작
        leaveQueue(authToken, current.token);
        break;
      }

      const updated = checkQueueStatus(authToken, current.token);
      if (!updated) {
        continue; // 일시적 오류: current 유지, 다음 폴링에서 재시도
      }
      current = updated;
    }

    if (current.phase === 'WAITING') {
      // 타임아웃으로 루프 탈출한 경우
      sleep(1 + Math.random() * 2);
      continue;
    }

    // 3. ALLOWED 도달
    queueAllowedRate.add(current.phase === 'ALLOWED');

    if (startedAsWaiting && current.phase === 'ALLOWED') {
      const waitedSeconds = (Date.now() - waitingStartedAt) / 1000;
      queueWaitSeconds.add(waitedSeconds);
      queuePromotedRate.add(true);
    }

    if (current.phase === 'ALLOWED') {
      // 슬롯 점유 (프론트엔드 관찰 시간 확보)
      sleep(SLOT_HOLD_SECONDS);

      // 4. 큐 이탈
      leaveQueue(authToken, current.token);
    }

    // 5. 지터 포함 대기 후 루프 재시작 → 큐 포화 상태 유지
    sleep(1 + Math.random() * 2);
  }
}
