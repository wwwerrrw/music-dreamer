const BASE_NAVIGATION = Object.freeze([
  { label: '发现音乐', shortLabel: '发现', path: '/discover', icon: 'compass' },
  { label: '搜索', shortLabel: '搜索', path: '/search', icon: 'search' },
  { label: '我的收藏', path: '/library/favorites', icon: 'heart' },
  { label: '我的歌单', shortLabel: '歌单', path: '/library/playlists', icon: 'list-music' },
  { label: '最近播放', path: '/library/history', icon: 'history' },
  { label: '消息', path: '/messages', icon: 'mail' }
]);

const MOBILE_NAVIGATION = Object.freeze([
  { label: '发现', path: '/discover', icon: 'compass' },
  { label: '搜索', path: '/search', icon: 'search' },
  { label: '消息', path: '/messages', icon: 'mail' },
  { label: '我的', path: '/library/favorites', icon: 'user' }
]);

export function defaultRouteFor(role) {
  return role === 'SINGER' ? '/artist/studio' : '/discover';
}

export function navigationFor(role) {
  const items = [...BASE_NAVIGATION];
  if (role === 'SINGER') {
    items.push({ label: '歌手工作台', path: '/artist/studio', icon: 'upload' });
  }
  return items;
}

export function mobileNavigation() {
  return MOBILE_NAVIGATION.map((item) => ({ ...item }));
}

export function resolveRouteAccess(route, session) {
  if (!route.requiresAuth) return { allow: true };
  if (!session) {
    return { allow: false, redirect: `/login?redirect=${encodeURIComponent(route.path)}` };
  }
  if (route.role && session.role !== route.role) {
    return { allow: false, redirect: defaultRouteFor(session.role) };
  }
  return { allow: true };
}
