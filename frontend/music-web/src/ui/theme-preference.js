export const THEME_STORAGE_KEY = 'music-dreamer:theme';

const POMEGRANATE_INK_THEME = 'pomegranate-ink';
const CLASSIC_THEME = 'classic';

export function loadThemePreference(storage) {
  return storage.getItem(THEME_STORAGE_KEY) === POMEGRANATE_INK_THEME;
}

export function saveThemePreference(storage, useInkTheme) {
  storage.setItem(THEME_STORAGE_KEY, useInkTheme ? POMEGRANATE_INK_THEME : CLASSIC_THEME);
}
