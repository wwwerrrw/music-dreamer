<template>
  <div class="page-shell library-page">
    <header class="page-header compact"><div><h1>我的音乐</h1><p class="page-subtitle">收藏歌曲、最近播放与自建歌单。</p></div></header>
    <LibraryTabs />
    <StateMessage v-if="state !== 'ready'" :state="state" :message="message" />
    <section v-else class="library-section">
      <div class="section-heading"><h2>收藏歌曲</h2><span>{{ tracks.length }} 首</span></div>
      <TrackRow v-for="(track, index) in tracks" :key="track.id" :track="track" :index="index" @play="play" />
      <p v-if="!tracks.length" class="empty-state">还没有收藏歌曲。</p>
    </section>
  </div>
</template>
<script>
import { API_PATHS, mapTrackSummary } from '@music-dreamer/shared';
import { api } from '../api.js';
import LibraryTabs from '../components/LibraryTabs.vue';
import StateMessage from '../components/StateMessage.vue';
import TrackRow from '../components/TrackRow.vue';
import { library } from '../library/client.js';
import { problemMessage } from '../ui/problem-message.js';

export default {
  name: 'FavoritesView',
  components: { LibraryTabs, StateMessage, TrackRow },
  data: () => ({ state: 'loading', message: '正在加载收藏…', tracks: [] }),
  async mounted() {
    try {
      const favorites = await library.favorites();
      this.tracks = await Promise.all(favorites.map(async (item) => mapTrackSummary((await api.get(API_PATHS.catalog.track(item.trackId))).data)));
      this.state = 'ready';
    } catch (problem) {
      this.state = 'error';
      this.message = problemMessage(problem);
    }
  },
  methods: {
    play(track) { this.$store.dispatch('player/playTrack', track); }
  }
};
</script>
