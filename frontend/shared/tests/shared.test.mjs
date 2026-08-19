import test from 'node:test';
import assert from 'node:assert/strict';
import axios from 'axios';

import { createApiClient, MEDIA_OBJECTS_PATH } from '../src/api/api-client.js';
import { toProblemDetail } from '../src/api/problem-detail.js';
import { ROLES, ACCOUNT_STATUS, TRACK_STATUS, MEDIA_TYPES } from '../src/contracts/constants.js';

test('api client adds bearer to application requests but never media object URLs', () => {
  const requests = [];
  const client = createApiClient({
    tokenProvider: () => 'opaque-session-token',
    adapter: async (config) => {
      requests.push(config);
      return { status: 200, data: { ok: true } };
    }
  });

  return Promise.all([
    client.get('/api/catalog/tracks'),
    client.get(`${MEDIA_OBJECTS_PATH}/42`, { mediaRequest: true })
  ]).then(() => {
    assert.equal(requests[0].headers.Authorization, 'Bearer opaque-session-token');
    assert.equal(requests[1].headers.Authorization, undefined);
    assert.equal(requests[1].url, `${MEDIA_OBJECTS_PATH}/42`);
  });
});

test('api client sends write payloads through the Axios data field', async () => {
  const requests = [];
  const client = createApiClient({
    adapter: async (config) => {
      requests.push(config);
      return { status: 204 };
    }
  });

  await client.post('/api/identity/registrations', { username: 'listener' });
  await client.put('/api/catalog/artists/me', { stageName: 'Night Signal' });

  assert.deepEqual(requests[0].data, { username: 'listener' });
  assert.deepEqual(requests[1].data, { stageName: 'Night Signal' });
  assert.equal('body' in requests[0], false);
  assert.equal('body' in requests[1], false);
});

test('api client keeps bearer authentication on media uploads', async () => {
  const requests = [];
  const client = createApiClient({
    tokenProvider: () => 'opaque-session-token',
    adapter: async (config) => {
      requests.push(config);
      return { status: 201, data: { id: 42 } };
    }
  });

  await client.post(MEDIA_OBJECTS_PATH, { file: 'audio fixture' });

  assert.equal(requests[0].headers.Authorization, 'Bearer opaque-session-token');
});

test('problem detail preserves status, code, detail, and trace id', () => {
  const problem = toProblemDetail({
    response: {
      status: 409,
      data: { code: 'CATALOG_CONFLICT', detail: 'Track state changed', traceId: 'trace-7' }
    }
  });

  assert.deepEqual(problem, {
    status: 409,
    code: 'CATALOG_CONFLICT',
    detail: 'Track state changed',
    traceId: 'trace-7'
  });
});

test('problem detail maps transport errors without collapsing semantic statuses', () => {
  assert.equal(toProblemDetail({ response: { status: 401, data: { code: 'IDENTITY_SESSION_INVALID' } } }).status, 401);
  assert.equal(toProblemDetail({ response: { status: 403, data: { code: 'IDENTITY_FORBIDDEN' } } }).status, 403);
  assert.equal(toProblemDetail({ response: { status: 503, data: { code: 'SERVICE_UNAVAILABLE' } } }).status, 503);
  assert.equal(toProblemDetail({ request: {} }).code, 'NETWORK_ERROR');
});

test('axios client rejects with a normalized problem detail', async () => {
  const client = createApiClient({
    axios,
    adapter: async () => Promise.reject({
      response: {
        status: 503,
        data: {
          code: 'SERVICE_UNAVAILABLE',
          detail: 'Catalog is unavailable',
          traceId: 'trace-503'
        }
      }
    })
  });

  await assert.rejects(client.get('/api/catalog/tracks/search'), (problem) => {
    assert.deepEqual(problem, {
      status: 503,
      code: 'SERVICE_UNAVAILABLE',
      detail: 'Catalog is unavailable',
      traceId: 'trace-503'
    });
    return true;
  });
});

test('shared contracts expose the server-owned role and status vocabulary', () => {
  assert.deepEqual(ROLES, { USER: 'USER', SINGER: 'SINGER', ADMIN: 'ADMIN' });
  assert.equal(ACCOUNT_STATUS.ACTIVE, 'ACTIVE');
  assert.equal(TRACK_STATUS.SUBMITTED, 'SUBMITTED');
  assert.equal(MEDIA_TYPES.AUDIO, 'AUDIO');
});
