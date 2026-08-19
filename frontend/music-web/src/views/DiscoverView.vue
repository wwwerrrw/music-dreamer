<template>
  <div class="page-shell">
    <header class="page-header">
      <div><h1>早上好</h1><p class="page-subtitle">按最新发布排序，发现今天可以播放的音乐。</p></div>
      <router-link class="search-link" to="/search"><Search :size="18" aria-hidden="true" /><span>搜索歌曲或歌手</span></router-link>
    </header>
    <StateMessage v-if="state !== 'ready'" :state="state" :message="stateMessage" />
    <template v-else>
      <section class="content-section">
        <div class="section-heading"><h2>今日新声</h2><router-link to="/search?sort=LATEST">查看全部</router-link></div>
        <div class="featured-grid">
          <article v-for="track in discovery.latest.tracks.slice(0, 4)" :key="track.id" class="featured-track" @click="play(track)">
            <button class="cover-button" type="button" :aria-label="`播放 ${track.title}`"><img :src="coverFor(track)" :alt="`${track.title} 封面`"><span><Play :size="20" fill="currentColor" /></span></button>
            <strong>{{ track.title }}</strong><small>{{ track.artistStageName }}</small>
          </article>
        </div>
      </section>
      <section class="content-section lower-content">
        <div class="track-list-section"><div class="section-heading"><h2>热门歌曲</h2><span>按播放量排序</span></div><TrackRow v-for="(track, index) in discovery.popular.tracks" :key="track.id" :track="track" :index="index" @play="play" /></div>
        <aside class="announcement"><span class="message-label">平台公告</span><p>本周音乐审核已经开放。歌手可在工作台提交新作品。</p><time>今天</time></aside>
      </section>
    </template>
  </div>
</template>

<script>
import { Play, Search } from 'lucide-vue';
import { api } from '../api.js';
import { loadDiscovery } from '../catalog/discovery.js';
import StateMessage from '../components/StateMessage.vue';
import TrackRow from '../components/TrackRow.vue';
import { problemMessage } from '../ui/problem-message.js';

export default {
  name: 'DiscoverView',
  components: { Play, Search, StateMessage, TrackRow },
  data: () => ({ state: 'loading', stateMessage: '正在加载音乐…', discovery: null }),
  async mounted() {
    try { this.discovery = await loadDiscovery(api); this.state = 'ready'; }
    catch (problem) { this.state = 'error'; this.stateMessage = problemMessage(problem); }
  },
  methods: {
    coverFor(track) { return track.coverMediaObjectId ? `/api/media/objects/${track.coverMediaObjectId}` : '/assets/studio-piano.jpg'; },
    play(track) { this.$store.dispatch('player/playTrack', track); }
  }
};
</script>
