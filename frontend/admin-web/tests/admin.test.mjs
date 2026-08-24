import test from 'node:test';
import assert from 'node:assert/strict';

import { resolveAdminAccess } from '../src/router/access.js';
import { createAdminApi } from '../src/api/admin-api.js';
import { confirmAction, adminProblemMessage, viewState } from '../src/ui/admin-model.js';

test('admin guard sends unauthenticated and non-admin sessions to login', () => {
  assert.deepEqual(resolveAdminAccess('/tracks', null), { allow: false, redirect: '/login?redirect=%2Ftracks' });
  assert.deepEqual(resolveAdminAccess('/tracks', { role: 'USER', status: 'ACTIVE' }), { allow: false, redirect: '/login?redirect=%2Ftracks' });
  assert.deepEqual(resolveAdminAccess('/tracks', { role: 'ADMIN', status: 'ACTIVE' }), { allow: true });
});

test('admin adapter maps user, track, and message operations to owned service paths', async () => {
  const calls = [];
  const api = {
    get: async (path) => { calls.push(['GET', path]); return { data: path.includes('tracks') ? { items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 } : [] }; },
    post: async (path, body) => { calls.push(body === undefined ? ['POST', path] : ['POST', path, body]); return { data: { id: 9 } }; }
  };
  const admin = createAdminApi(api);
  await admin.users();
  await admin.tracks({ status: 'SUBMITTED', page: 0, size: 20 });
  await admin.lockUser(16);
  await admin.promoteSinger(16);
  await admin.publishTrack(8);
  await admin.lockTrack(8);
  await admin.publishMessage({ subject: '公告', body: '内容' });
  assert.deepEqual(calls, [
    ['GET', '/api/identity/admin/users'],
    ['GET', '/api/catalog/admin/tracks?status=SUBMITTED&page=0&size=20'],
    ['POST', '/api/identity/admin/users/16/lock'],
    ['POST', '/api/identity/admin/users/16/promote-singer'],
    ['POST', '/api/catalog/admin/tracks/8/publish'],
    ['POST', '/api/catalog/admin/tracks/8/lock'],
    ['POST', '/api/interaction/admin/messages', { subject: '公告', body: '内容' }]
  ]);
});

test('dangerous action cancellation does not call the API', async () => {
  let calls = 0;
  const action = () => { calls += 1; };
  assert.equal(await confirmAction(false, action), false);
  assert.equal(calls, 0);
  assert.equal(await confirmAction(true, action), true);
  assert.equal(calls, 1);
});

test('admin pages keep independent states and distinct failure messages', () => {
  assert.deepEqual(viewState('users', 'loading'), { page: 'users', kind: 'loading' });
  assert.deepEqual(viewState('tracks', 'empty'), { page: 'tracks', kind: 'empty' });
  assert.deepEqual(viewState('messages', { status: 503 }), { page: 'messages', kind: 'error', code: 'RETRY' });
  assert.equal(adminProblemMessage({ status: 403 }), '管理员权限不足。');
  assert.equal(adminProblemMessage({ status: 409 }), '服务端状态已变化，请刷新后重试。');
  assert.equal(adminProblemMessage({ status: 503 }), '服务暂时不可用，请稍后重试。');
});
