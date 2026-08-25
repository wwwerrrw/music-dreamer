<template>
  <div class="page-shell library-page">
    <header class="page-header compact"><div><h1>我的音乐</h1><p class="page-subtitle">收藏歌曲、最近播放与自建歌单。</p></div></header>
    <LibraryTabs />
    <StateMessage v-if="state !== 'ready'" :state="state" :message="message" />
    <section v-else class="library-section">
      <div class="section-heading"><h2>最近播放</h2><span>{{ tracks.length }} 首</span></div>
      <TrackRow v-for="(track, index) in tracks" :key="track.id" :track="track" :index="index" @play="play" />
      <p v-if="!tracks.length" class="empty-state">还没有播放记录。</p>
    </section>
  </div>
</template>
<script>
import { api } from '../api.js';
import LibraryTabs from '../components/LibraryTabs.vue';
import StateMessage from '../components/StateMessage.vue';
import TrackRow from '../components/TrackRow.vue';
import { library } from '../library/client.js';
import { loadHistoryTracks } from '../library/history-loader.js';
import { problemMessage } from '../ui/problem-message.js';

export default {
  name: 'HistoryView',
  components: { LibraryTabs, StateMessage, TrackRow },
  data: () => ({ state: 'loading', message: '正在加载播放记录…', tracks: [] }),
  async mounted() {
    try {
      this.tracks = await loadHistoryTracks({ api, library });
      this.state = 'ready';
    } catch (problem) {
      this.state = 'error';
      this.message = problemMessage(problem);
    }
  },
  methods: {
    play(track) { this.$store.dispatch('player/playTrack', { track, queue: this.tracks }); }
  }
};
</script>
