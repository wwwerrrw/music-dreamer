<template>
  <div
    class="app-shell"
    :class="{
      'has-player': Boolean(player.track),
      'theme-pomegranate-ink': useInkTheme
    }"
  >
    <aside class="desktop-sidebar" aria-label="主导航">
      <router-link class="brand" to="/discover" aria-label="悦享音乐发现页">
        <span class="brand-mark"><Music2 :size="22" :stroke-width="2.4" aria-hidden="true" /></span>
        <span class="brand-copy">悦享音乐</span>
      </router-link>
      <nav class="sidebar-links">
        <router-link
          v-for="item in navigation"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          :class="{ 'starts-nav-group': startsNavGroup(item) }"
        >
          <component :is="iconFor(item.icon)" :size="20" :stroke-width="2" aria-hidden="true" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
      <label class="sidebar-theme">
        <input
          class="theme-switch-input"
          type="checkbox"
          role="switch"
          :checked="useInkTheme"
          :aria-label="useInkTheme ? '切换到红白主题' : '切换到榴花红墨色主题'"
          @change="toggleTheme"
        >
        <span class="theme-switch-track" aria-hidden="true">
          <Sun class="theme-switch-icon sun" :size="13" :stroke-width="2" />
          <Moon class="theme-switch-icon moon" :size="12" :stroke-width="2" />
          <span class="theme-switch-thumb"></span>
        </span>
      </label>
      <div class="sidebar-account">
        <router-link class="sidebar-account-link" to="/profile" aria-label="打开个人主页" title="个人主页">
          <UserRound :size="20" aria-hidden="true" />
          <span>{{ roleLabel }}</span>
        </router-link>
        <button class="icon-button" type="button" aria-label="退出登录" title="退出登录" @click="logout">
          <LogOut :size="20" aria-hidden="true" />
        </button>
      </div>
    </aside>

    <main class="app-main"><router-view :key="routeViewKey" /></main>

    <MiniPlayer v-bind="player" @cycle-mode="cycleMode" @previous="previous" @toggle="toggle" @next="next" @seek="seek" @volume="setVolume" />
    <audio ref="audio" preload="metadata" @loadedmetadata="onMetadata" @timeupdate="onTimeupdate" @play="setPlaying(true)" @pause="onPause" @ended="onEnded" />

    <nav class="mobile-navigation" aria-label="移动端主导航">
      <router-link v-for="item in mobileNavigation" :key="item.path" :to="item.path">
        <component :is="item.icon" :size="20" :stroke-width="2" aria-hidden="true" />
        <span>{{ item.label }}</span>
      </router-link>
    </nav>
  </div>
</template>

<script>
import {
  Compass, Heart, History, ListMusic, LogOut, Mail, Moon, Music2, Search, Sun, Upload, UserRound
} from 'lucide-vue';
import { syncTrackDetailRoute, trackRouteViewKey } from '../player/media.js';
import { navigationFor } from '../router/access.js';
import { loadThemePreference, saveThemePreference } from '../ui/theme-preference.js';
import MiniPlayer from './MiniPlayer.vue';

const icons = { compass: Compass, heart: Heart, history: History, 'list-music': ListMusic, mail: Mail, search: Search, upload: Upload };

export default {
  name: 'AppShell',
  components: { LogOut, MiniPlayer, Moon, Music2, Sun, UserRound },
  data: () => ({ useInkTheme: false }),
  computed: {
    session() { return this.$store.state.auth.session; },
    navigation() { return navigationFor(this.session?.role); },
    roleLabel() { return this.session?.role === 'SINGER' ? '歌手账号' : '普通用户'; },
    player() { return this.$store.state.player; },
    currentPlayerTrackId() { return this.player.track?.id ?? null; },
    routeViewKey() { return trackRouteViewKey(this.$route); },
    mobileNavigation() {
      return [
        { label: '发现', path: '/discover', icon: Compass },
        { label: '搜索', path: '/search', icon: Search },
        { label: '歌单', path: '/library/playlists', icon: ListMusic },
        { label: '我的', path: '/profile', icon: UserRound }
      ];
    }
  },
  watch: {
    currentPlayerTrackId() {
      syncTrackDetailRoute({
        route: this.$route,
        track: this.player.track,
        replace: (location) => this.$router.replace(location, () => {}, () => {})
      });
    }
  },
  mounted() {
    this.useInkTheme = loadThemePreference(window.localStorage);
    this.$store.commit('player/initialize', this.$refs.audio);
    window.addEventListener('pagehide', this.onPageHide);
  },
  beforeDestroy() {
    window.removeEventListener('pagehide', this.onPageHide);
    this.$store.dispatch('player/persistHistory', 'pagehide');
    this.$refs.audio?.pause();
  },
  methods: {
    iconFor(name) { return icons[name] || Compass; },
    startsNavGroup(item) { return item.path === '/library/favorites' || item.path === '/artist/studio'; },
    toggleTheme(event) {
      this.useInkTheme = event.target.checked;
      saveThemePreference(window.localStorage, this.useInkTheme);
    },
    async logout() {
      await this.$store.dispatch('auth/logout');
      this.$router.replace('/login');
    },
    toggle() { this.$store.dispatch('player/toggle'); },
    previous() { this.$store.dispatch('player/previous'); },
    next() { this.$store.dispatch('player/next'); },
    cycleMode() { this.$store.dispatch('player/cycleMode'); },
    seek(value) { this.$store.dispatch('player/seek', value); },
    setVolume(value) { this.$store.dispatch('player/setVolume', value); },
    setPlaying(value) { this.$store.commit('player/setPlaying', value); },
    onMetadata() { this.$store.commit('player/setDuration', this.$refs.audio.duration || 0); },
    onTimeupdate() { this.$store.commit('player/setTime', this.$refs.audio.currentTime || 0); this.$store.dispatch('player/persistHistory', 'timeupdate'); },
    onPause() { this.setPlaying(false); this.$store.dispatch('player/persistHistory', 'pause'); },
    onEnded() { this.$store.dispatch('player/ended'); },
    onPageHide() { this.$store.dispatch('player/persistHistory', 'pagehide'); }
  }
};
</script>
