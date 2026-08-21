<template>
  <div class="app-shell" :class="{ 'has-player': Boolean(player.track) }">
    <aside class="desktop-sidebar" aria-label="主导航">
      <router-link class="brand" to="/discover" aria-label="悦享音乐发现页">
        <span class="brand-mark"><Music2 :size="22" :stroke-width="2.4" aria-hidden="true" /></span>
        <span class="brand-copy">悦享音乐</span>
      </router-link>
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

    <MiniPlayer v-bind="player" @previous="previous" @next="next" @toggle="toggle" @seek="seek" @volume="setVolume" />
    <audio ref="audio" preload="metadata" @loadedmetadata="onMetadata" @timeupdate="onTimeupdate" @play="setPlaying(true)" @pause="onPause" @ended="onEnded" />

    <nav class="mobile-navigation" aria-label="移动端主导航">
      <router-link v-for="item in mobileNavigation" :key="item.path" :to="item.path">
        <component :is="iconFor(item.icon)" :size="20" :stroke-width="2" aria-hidden="true" />
        <span>{{ item.label }}</span>
      </router-link>
    </nav>
  </div>
</template>

<script>
import {
  Compass, Heart, History, ListMusic, LogOut, Mail, Music2, Search, Upload, UserRound
} from 'lucide-vue';
import { mobileNavigation as mobileNavigationItems, navigationFor } from '../router/access.js';
import MiniPlayer from './MiniPlayer.vue';

const icons = { compass: Compass, heart: Heart, history: History, 'list-music': ListMusic, mail: Mail, search: Search, upload: Upload, user: UserRound };

export default {
  name: 'AppShell',
  components: { LogOut, MiniPlayer, Music2, UserRound },
  computed: {
    session() { return this.$store.state.auth.session; },
    navigation() { return navigationFor(this.session?.role); },
    roleLabel() { return this.session?.role === 'SINGER' ? '歌手账号' : '普通用户'; },
    player() {
      const player = this.$store.state.player;
      return {
        track: player.track,
        playing: player.playing,
        currentTime: player.currentTime,
        duration: player.duration,
        volume: player.volume,
        hasPrevious: player.queueIndex > 0,
        hasNext: player.queueIndex >= 0 && player.queueIndex < player.queue.length - 1
      };
    },
    mobileNavigation() { return mobileNavigationItems(); }
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
    previous() { this.$store.dispatch('player/previous'); },
    next() { this.$store.dispatch('player/next'); },
    seek(value) { this.$store.dispatch('player/seek', value); },
    setVolume(value) { this.$store.dispatch('player/setVolume', value); },
    setPlaying(value) { this.$store.commit('player/setPlaying', value); },
    onMetadata() { this.$store.commit('player/setDuration', this.$refs.audio.duration || 0); },
    onTimeupdate() { this.$store.commit('player/setTime', this.$refs.audio.currentTime || 0); this.$store.dispatch('player/persistHistory', 'timeupdate'); },
    onPause() { this.setPlaying(false); this.$store.dispatch('player/persistHistory', 'pause'); },
    async onEnded() { if (!await this.$store.dispatch('player/next')) this.setPlaying(false); },
    onPageHide() { this.$store.dispatch('player/persistHistory', 'pagehide'); }
  }
};
</script>
