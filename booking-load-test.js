import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const EVENT_ID = Number(__ENV.EVENT_ID || 1);
const USER_START = Number(__ENV.USER_START || 1);
const USER_COUNT = Number(__ENV.USER_COUNT || 150);
const DURATION = __ENV.DURATION || '30s';

const queueAllowedRate = new Rate('queue_allowed_rate');
const queueWaitingRate = new Rate('queue_waiting_rate');
const queueEnterFailureCount = new Counter('queue_enter_failure_count');

export const options = {
  vus: USER_COUNT,
  duration: DURATION,
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
  queueWaitingRate.add(current.phase === 'WAITING');

  for (let attempt = 0; attempt < 20 && current.phase === 'WAITING'; attempt += 1) {
    sleep(1);
    current = checkQueueStatus(authToken, current.token);
  }

  queueAllowedRate.add(current.phase === 'ALLOWED');
  sleep(1);
}
