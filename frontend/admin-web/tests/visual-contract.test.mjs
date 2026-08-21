import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

function source(relativePath) {
  return readFileSync(new URL(`../src/${relativePath}`, import.meta.url), 'utf8');
}

const shell = source('components/AdminShell.vue');
const login = source('views/LoginView.vue');
const tracks = source('views/TracksView.vue');
const users = source('views/UsersView.vue');
const messages = source('views/MessagesView.vue');
const css = source('styles/admin.css');
const documentShell = readFileSync(new URL('../public/index.html', import.meta.url), 'utf8');

test('admin shell and login use the approved Chinese brand and light chrome', () => {
  assert.match(documentShell, /<title>悦享音乐管理后台<\/title>/);
  assert.match(documentShell, /name="theme-color" content="#fff8f7"/);
  assert.match(shell, /悦享音乐/);
  assert.match(shell, /admin-topbar/);
  assert.doesNotMatch(shell, />MD\s*<span>管理/);
  assert.match(login, /管理员登录/);
  assert.match(login, /showPassword/);
  assert.match(css, /var\(--md-primary\)/);
});

test('admin pages expose the approved review, user, and publishing workflows', () => {
  assert.match(tracks, /待审核队列/);
  assert.match(tracks, /查看详情/);
  assert.match(tracks, /review-drawer/);
  assert.match(users, /用户管理/);
  assert.match(users, /user-search/);
  assert.match(messages, /发布消息/);
  assert.match(messages, /message-preview/);
  assert.match(css, /\.review-drawer/);
  assert.match(css, /\.message-publish-layout/);
});
