import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

function source(relativePath) {
  return readFileSync(new URL(`../src/${relativePath}`, import.meta.url), 'utf8');
}

const appShell = source('components/AppShell.vue');
const appCss = source('styles/app.css');
const login = source('views/LoginView.vue');
const register = source('views/RegisterView.vue');
const activate = source('views/ActivateView.vue');

test('music shell uses the approved brand and responsive chrome', () => {
  assert.match(appShell, /悦享音乐/);
  assert.doesNotMatch(appShell, /MUSIC\s*<span>DREAMER/);
  assert.match(appCss, /var\(--md-primary\)/);
  assert.match(appCss, /\.mobile-navigation/);
  assert.match(appCss, /padding-bottom:\s*calc\(/);
});

test('auth views retain their workflows while exposing the approved Chinese landmarks', () => {
  assert.match(login, /欢迎回来/);
  assert.match(login, /账号或邮箱/);
  assert.match(login, /showPassword/);
  assert.match(register, /创建账号/);
  assert.match(register, /确认密码/);
  assert.match(activate, /账号启用成功/);
  assert.match(activate, /前往登录/);
});
