import test from 'node:test';
import assert from 'node:assert/strict';

import {
  clearSession,
  loadSession,
  saveSession,
  sessionErrorAction
} from '../src/auth/session.js';
import {
  defaultRouteFor,
  navigationFor,
  resolveRouteAccess
} from '../src/router/access.js';
import { selectActivationMessage } from '../src/auth/activation.js';
import { login, registerAndActivate } from '../src/auth/workflows.js';
import * as authWorkflows from '../src/auth/workflows.js';
import { routeRecords } from '../src/router/routes.js';
import { problemMessage } from '../src/ui/problem-message.js';

function memoryStorage() {
  const values = new Map();
  return {
    getItem: (key) => values.has(key) ? values.get(key) : null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key),
    snapshot: () => new Map(values)
  };
}

const activeUser = {
  token: 'opaque-token',
  expiresAt: '2099-08-19T08:00:00Z',
  userId: 15,
  role: 'USER',
  status: 'ACTIVE'
};

test('session storage keeps only the server-issued session contract', () => {
  const storage = memoryStorage();
  saveSession(storage, { ...activeUser, password: 'no', rawToken: 'activation-secret' });

  assert.deepEqual(loadSession(storage), activeUser);
  assert.doesNotMatch([...storage.snapshot().values()].join(''), /password|activation-secret/);
});

test('route access sends unauthenticated users to login and rejects non-singers', () => {
  assert.deepEqual(resolveRouteAccess({ path: '/discover', requiresAuth: true }, null), {
    allow: false,
    redirect: '/login?redirect=%2Fdiscover'
  });
  assert.deepEqual(resolveRouteAccess({ path: '/artist/studio', requiresAuth: true, role: 'SINGER' }, activeUser), {
    allow: false,
    redirect: '/discover'
  });
});

test('login destinations and navigation reflect the server-issued role', () => {
  assert.equal(defaultRouteFor('USER'), '/discover');
  assert.equal(defaultRouteFor('SINGER'), '/artist/studio');
  assert.equal(navigationFor('USER').some((item) => item.path === '/artist/studio'), false);
  assert.equal(navigationFor('SINGER').some((item) => item.path === '/artist/studio'), true);
});

test('401 clears the session while 403 preserves it', () => {
  const storage = memoryStorage();
  saveSession(storage, activeUser);
  assert.equal(sessionErrorAction({ status: 403 }, storage), 'FORBIDDEN');
  assert.deepEqual(loadSession(storage), activeUser);

  assert.equal(sessionErrorAction({ status: 401 }, storage), 'LOGIN');
  assert.equal(loadSession(storage), null);
  clearSession(storage);
});

test('activation selection matches the exact registration email and newest expiry', () => {
  const selected = selectActivationMessage([
    { email: 'other@example.com', rawToken: 'wrong', expiresAt: '2099-08-19T09:00:00Z' },
    { email: 'new@example.com', rawToken: 'older', expiresAt: '2099-08-19T08:00:00Z' },
    { email: 'new@example.com', rawToken: 'current', expiresAt: '2099-08-19T10:00:00Z' }
  ], 'new@example.com');

  assert.deepEqual(selected, { email: 'new@example.com', rawToken: 'current', expiresAt: '2099-08-19T10:00:00Z' });
});

test('login posts credentials, persists the mapped session, and returns the role destination', async () => {
  const calls = [];
  const storage = memoryStorage();
  const api = {
    post: async (path, body) => {
      calls.push({ path, body });
      return { data: { ...activeUser, role: 'SINGER' } };
    }
  };

  const result = await login({ api, storage, credentials: { login: 'artist', password: 'secret' } });

  assert.deepEqual(calls, [{ path: '/api/identity/sessions', body: { login: 'artist', password: 'secret' } }]);
  assert.equal(result.redirect, '/artist/studio');
  assert.equal(result.session.password, undefined);
  assert.equal(loadSession(storage).role, 'SINGER');
});

test('registration activates only the exact email message without returning the activation token', async () => {
  const calls = [];
  const api = {
    post: async (path, body) => {
      calls.push({ method: 'POST', path, body });
      if (path.endsWith('/registrations')) return { data: { userId: 27 } };
      return { status: 204 };
    },
    get: async (path) => {
      calls.push({ method: 'GET', path });
      return { data: [
        { email: 'other@example.com', rawToken: 'wrong', expiresAt: '2099-08-19T10:00:00Z' },
        { email: 'new@example.com', rawToken: 'activation-only', expiresAt: '2099-08-19T10:00:00Z' }
      ] };
    }
  };

  const result = await registerAndActivate({
    api,
    registration: { username: 'new-user', email: 'new@example.com', password: 'secret' }
  });

  assert.deepEqual(result, { userId: 27, email: 'new@example.com' });
  assert.deepEqual(calls.at(-1), {
    method: 'POST',
    path: '/api/identity/activations',
    body: { token: 'activation-only' }
  });
});

test('registration password confirmation reports a stable Chinese validation message', () => {
  assert.equal(typeof authWorkflows.registrationPasswordError, 'function');
  assert.equal(authWorkflows.registrationPasswordError('secret-123', 'different'), '两次输入的密码不一致。');
  assert.equal(authWorkflows.registrationPasswordError('secret-123', 'secret-123'), '');
});

test('route records cover all approved public, library, artist, and detail routes', () => {
  const routes = new Map(routeRecords.map((route) => [route.path, route]));
  for (const path of [
    '/login', '/register', '/activate', '/discover', '/search', '/tracks/:trackId',
    '/artists/:userId', '/profile', '/library/favorites', '/library/playlists',
    '/library/playlists/:playlistId', '/library/history', '/messages',
    '/artist/profile', '/artist/studio'
  ]) {
    assert.equal(routes.has(path), true, `missing route ${path}`);
  }
  assert.equal(routes.get('/artist/studio').meta.role, 'SINGER');
  assert.equal(routes.get('/discover').meta.requiresAuth, true);
  assert.equal(routes.get('/profile').meta.requiresAuth, true);
  assert.equal(routes.get('/login').meta.requiresAuth, false);
});

test('identity and availability failures keep distinct user-facing semantics', () => {
  assert.equal(problemMessage({ status: 401 }), '登录状态已失效，请重新登录。');
  assert.equal(problemMessage({ status: 403 }), '当前账号没有执行此操作的权限。');
  assert.equal(problemMessage({ status: 409 }), '内容状态已变化，请刷新后重试。');
  assert.equal(problemMessage({ status: 503 }), '服务暂时不可用，请稍后重试。');
  assert.equal(problemMessage({ status: 500, traceId: 'trace-9' }), '请求失败（追踪号：trace-9）');
});
