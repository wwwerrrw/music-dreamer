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
const trackRow = source('components/TrackRow.vue');
const discover = source('views/DiscoverView.vue');
const search = source('views/SearchView.vue');
const track = source('views/TrackView.vue');
const artist = source('views/ArtistView.vue');
const contentCss = source('styles/content.css');
const favorites = source('views/FavoritesView.vue');
const history = source('views/HistoryView.vue');
const playlists = source('views/PlaylistsView.vue');
const playlist = source('views/PlaylistView.vue');
const messages = source('views/MessagesView.vue');
const artistProfile = source('views/ArtistProfileView.vue');
const artistStudio = source('views/ArtistStudioView.vue');
const documentShell = readFileSync(new URL('../index.html', import.meta.url), 'utf8');

test('music shell uses the approved brand and responsive chrome', () => {
  assert.match(documentShell, /<title>悦享音乐<\/title>/);
  assert.match(documentShell, /name="theme-color" content="#fff8f7"/);
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

test('catalog views expose the approved Stitch landmarks without personalization claims', () => {
  assert.match(discover, /discovery-hero/);
  assert.match(discover, /今日新声/);
  assert.match(discover, /热门推荐/);
  assert.match(discover, /听歌排行/);
  assert.doesNotMatch(discover, /个性化推荐/);

  assert.match(search, /search-filters/);
  assert.match(search, /搜索结果/);

  assert.match(track, /track-detail-layout/);
  assert.match(track, /歌词/);
  assert.match(track, /暂无歌词/);
  assert.match(track, /接下来播放/);

  assert.match(artist, /artist-about/);
  assert.match(artist, /热门单曲/);
  assert.match(trackRow, /track-cover/);
  assert.match(contentCss, /\.discovery-hero/);
  assert.match(contentCss, /@media\s*\(max-width:\s*768px\)/);
});

test('library, message, and studio views keep the approved workflow landmarks', () => {
  const libraryViews = [favorites, history, playlists, playlist].join('\n');
  for (const label of ['收藏歌曲', '最近播放', '我的歌单']) assert.match(libraryViews, new RegExp(label));
  assert.match(playlists, /new-playlist-card/);
  assert.match(playlist, /playlist-track-row/);

  assert.match(messages, /消息中心/);
  assert.match(messages, /message-center-layout/);
  assert.match(messages, /message-detail-panel/);
  assert.match(messages, /全部标记已读/);

  for (const label of ['媒体文件', '基本信息', '发布设置', '保存草稿', '提交审核']) {
    assert.match(artistStudio, new RegExp(label));
  }
  assert.match(artistProfile, /profile-preview/);
  assert.match(contentCss, /\.library-tabs/);
  assert.match(contentCss, /\.studio-layout/);
});
