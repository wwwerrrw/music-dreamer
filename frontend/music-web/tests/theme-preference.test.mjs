import test from 'node:test';
import assert from 'node:assert/strict';
import {
  loadThemePreference,
  saveThemePreference,
  THEME_STORAGE_KEY
} from '../src/ui/theme-preference.js';

function storageWith(initialValue = null) {
  const values = new Map();
  if (initialValue !== null) values.set(THEME_STORAGE_KEY, initialValue);

  return {
    getItem(key) { return values.has(key) ? values.get(key) : null; },
    setItem(key, value) { values.set(key, value); }
  };
}

test('theme preference restores only the pomegranate ink theme', () => {
  assert.equal(loadThemePreference(storageWith('pomegranate-ink')), true);
  assert.equal(loadThemePreference(storageWith('classic')), false);
  assert.equal(loadThemePreference(storageWith('unexpected-theme')), false);
  assert.equal(loadThemePreference(storageWith()), false);
});

test('theme preference persists the selected desktop theme', () => {
  const storage = storageWith();

  saveThemePreference(storage, true);
  assert.equal(storage.getItem(THEME_STORAGE_KEY), 'pomegranate-ink');

  saveThemePreference(storage, false);
  assert.equal(storage.getItem(THEME_STORAGE_KEY), 'classic');
});
