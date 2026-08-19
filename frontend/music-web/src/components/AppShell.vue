<template>
  <div class="app-shell">
    <aside class="desktop-sidebar" aria-label="主导航">
      <router-link class="brand" to="/discover">MUSIC <span>DREAMER</span></router-link>
      <nav class="sidebar-links">
        <router-link v-for="item in navigation" :key="item.path" :to="item.path" class="nav-link">
          <component :is="iconFor(item.icon)" :size="20" :stroke-width="2" aria-hidden="true" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
      <div class="sidebar-account">
        <UserRound :size="20" aria-hidden="true" />
        <span>{{ roleLabel }}</span>
        <button class="icon-button" type="button" aria-label="退出登录" title="退出登录" @click="logout">
          <LogOut :size="20" aria-hidden="true" />
        </button>
      </div>
    </aside>

    <main class="app-main"><router-view /></main>

    <MiniPlayer v-bind="player" @toggle="toggle" @seek="seek" @volume="setVolume" />
    <audio ref="audio" preload="metadata" @loadedmetadata="onMetadata" @timeupdate="onTimeupdate" @play="setPlaying(true)" @pause="onPause" />

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
  Compass, Heart, History, ListMusic, LogOut, Mail, Search, Upload, UserRound
} from 'lucide-vue';
import { navigationFor } from '../router/access.js';
import MiniPlayer from './MiniPlayer.vue';

const icons = { compass: Compass, heart: Heart, history: History, 'list-music': ListMusic, mail: Mail, search: Search, upload: Upload };

export default {
  name: 'AppShell',
  components: { LogOut, MiniPlayer, UserRound },
  computed: {
    session() { return this.$store.state.auth.session; },
    navigation() { return navigationFor(this.session?.role); },
    roleLabel() { return this.session?.role === 'SINGER' ? '歌手账号' : '普通用户'; },
    player() { return this.$store.state.player; },
    mobileNavigation() {
      return [
        { label: '发现', path: '/discover', icon: Compass },
        { label: '搜索', path: '/search', icon: Search },
        { label: '歌单', path: '/library/playlists', icon: ListMusic },
        { label: '我的', path: '/library/favorites', icon: UserRound }
      ];
    }
  },
  mounted() {
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
    async logout() {
      await this.$store.dispatch('auth/logout');
      this.$router.replace('/login');
    },
    toggle() { this.$store.dispatch('player/toggle'); },
    seek(value) { this.$store.dispatch('player/seek', value); },
    setVolume(value) { this.$store.dispatch('player/setVolume', value); },
    setPlaying(value) { this.$store.commit('player/setPlaying', value); },
    onMetadata() { this.$store.commit('player/setDuration', this.$refs.audio.duration || 0); },
    onTimeupdate() { this.$store.commit('player/setTime', this.$refs.audio.currentTime || 0); this.$store.dispatch('player/persistHistory', 'timeupdate'); },
    onPause() { this.setPlaying(false); this.$store.dispatch('player/persistHistory', 'pause'); },
    onPageHide() { this.$store.dispatch('player/persistHistory', 'pagehide'); }
  }
};
</script>
