import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const EVENT_ID = Number(__ENV.EVENT_ID || 1);
const USER_START = Number(__ENV.USER_START || 2);
const USER_COUNT = Number(__ENV.USER_COUNT || 150);
const DURATION = __ENV.DURATION || '180s';
const LEAVE_RATE_PER_SEC = Number(__ENV.LEAVE_RATE_PER_SEC || 5);
const MAX_ACTIVE_SLOTS = Number(__ENV.MAX_ACTIVE_SLOTS || 100);
const MAX_WAIT_SECONDS = Number(__ENV.MAX_WAIT_SECONDS || 600);
const POLL_INTERVAL_SECONDS = Number(__ENV.POLL_INTERVAL_SECONDS || 1);

const queueAllowedRate = new Rate('queue_allowed_rate');
const queueWaitingRate = new Rate('queue_waiting_rate');
const queuePromotedRate = new Rate('queue_promoted_rate');
const queueLeaveSuccessRate = new Rate('queue_leave_success_rate');
const queueEnterFailureCount = new Counter('queue_enter_failure_count');
const queueStuckTimeoutCount = new Counter('queue_stuck_timeout_count');
const queueLeaveFailureCount = new Counter('queue_leave_failure_count');
const queueWaitSeconds = new Trend('queue_wait_seconds');

export const options = {
  scenarios: {
    queue_once_per_user: {
      executor: 'per-vu-iterations',
      vus: USER_COUNT,
      iterations: 1,
      maxDuration: DURATION,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<1200'],
  },
};

function parseApiResponse(res, apiName) {
  let body;
  try {
    body = res.json();
  } catch (e) {
    fail(`${apiName}: JSON 파싱 실패 - ${e}`);
  }

  if (!body || typeof body !== 'object' || !('data' in body)) {
    fail(`${apiName}: 응답 포맷이 예상과 다릅니다.`);
  }

  return body.data;
}

function login(email, password) {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email, password }),
    {
      headers: { 'Content-Type': 'application/json' },
    }
  );

  check(res, {
    'login status is 200': (r) => r.status === 200,
  });

  const data = parseApiResponse(res, 'login');
  if (!data.token) {
    fail(`login: token 누락 (${email})`);
  }

  return data.token;
}

function enterQueue(authToken) {
  const res = http.post(`${BASE_URL}/api/v1/queue/events/${EVENT_ID}`, null, {
    headers: {
      Authorization: `Bearer ${authToken}`,
    },
  });

  const ok = check(res, {
    'enterQueue status is 200': (r) => r.status === 200,
  });

  if (!ok) {
    queueEnterFailureCount.add(1);
    return null;
  }

  return parseApiResponse(res, 'enterQueue');
}

function checkQueueStatus(authToken, queueToken) {
  const encodedToken = encodeURIComponent(queueToken);
  const res = http.get(`${BASE_URL}/api/v1/queue/events/${EVENT_ID}?token=${encodedToken}`, {
    headers: {
      Authorization: `Bearer ${authToken}`,
    },
  });

  check(res, {
    'checkStatus status is 200': (r) => r.status === 200,
  });

  return parseApiResponse(res, 'checkQueueStatus');
}

function leaveQueue(authToken, queueToken) {
  const res = http.post(
    `${BASE_URL}/api/v1/queue/events/${EVENT_ID}/leave`,
    JSON.stringify({
      queueToken,
    }),
    {
      headers: {
        Authorization: `Bearer ${authToken}`,
        'Content-Type': 'application/json',
      },
    }
  );

  const ok = check(res, {
    'leaveQueue status is 200': (r) => r.status === 200,
  });
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

export function setup() {
  const tokens = [];

  for (let i = USER_START; i < USER_START + USER_COUNT; i += 1) {
    tokens.push(login(`user${i}@gmail.com`, `password${i}`));
  }

  return { tokens };
}

export default function (data) {
  const authToken = data.tokens[(__VU - 1) % data.tokens.length];

  const queueStatus = enterQueue(authToken);
  if (!queueStatus?.token) {
    return;
  }

  let current = queueStatus;
  const startedAsWaiting = current.phase === 'WAITING';
  const waitingStartedAt = Date.now();
  queueWaitingRate.add(current.phase === 'WAITING');

  while (current.phase === 'WAITING') {
    sleep(POLL_INTERVAL_SECONDS);
    current = checkQueueStatus(authToken, current.token);
    const elapsedSeconds = (Date.now() - waitingStartedAt) / 1000;
    if (elapsedSeconds > MAX_WAIT_SECONDS) {
      queueStuckTimeoutCount.add(1);
      queuePromotedRate.add(false);
      return;
    }
  }

  queueAllowedRate.add(current.phase === 'ALLOWED');
  if (startedAsWaiting) {
    const waitedSeconds = (Date.now() - waitingStartedAt) / 1000;
    queueWaitSeconds.add(waitedSeconds);
    queuePromotedRate.add(current.phase === 'ALLOWED');
  }
  if (current.phase === 'ALLOWED') {
    // Stagger leave timing by VU so slots are released from the beginning.
    const leaveDelay = LEAVE_RATE_PER_SEC > 0
      ? ((__VU - 1) % MAX_ACTIVE_SLOTS) / LEAVE_RATE_PER_SEC
      : 0;
    if (leaveDelay > 0) {
      sleep(leaveDelay);
    }
    leaveQueue(authToken, current.token);
  }
  sleep(1);
}
