import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');

const authLayout = read('../src/components/AuthLayout.vue');
const loginView = read('../src/views/LoginView.vue');
const appShell = read('../src/components/AppShell.vue');
const appStyles = read('../src/styles/app.css');

test('login opts into a real-image asymmetric auth layout', () => {
  assert.match(loginView, /<AuthLayout\s+immersive\b/);
  assert.match(authLayout, /class="auth-visual"/);
  assert.match(authLayout, /src="\/assets\/studio-piano\.jpg"/);
  assert.match(appStyles, /\.auth-layout\.is-immersive\s*\{/);
});

test('desktop app shell uses a narrow sidebar with visual navigation groups', () => {
  assert.match(appShell, /starts-nav-group/);
  assert.match(appStyles, /--md-sidebar-width:\s*212px/);
  assert.match(appStyles, /\.nav-link\.starts-nav-group/);
});

test('sidebar account keeps the profile label beside a fixed logout action', () => {
  assert.match(
    appStyles,
    /\.sidebar-account\s*\{[^}]*grid-template-columns:\s*minmax\(0,\s*1fr\)\s+34px/s
  );
  assert.match(
    appStyles,
    /\.sidebar-account-link\s*>\s*span\s*\{[^}]*white-space:\s*nowrap/s
  );
});
