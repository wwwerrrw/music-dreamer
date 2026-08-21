<template>
  <div class="page-shell discovery-page">
    <header class="page-header">
      <div><h1>发现音乐</h1><p class="page-subtitle">准备好发现新音乐了吗？</p></div>
      <router-link class="search-link" to="/search"><Search :size="18" aria-hidden="true" /><span>搜索歌曲或歌手</span></router-link>
    </header>
    <StateMessage v-if="state !== 'ready'" :state="state" :message="stateMessage" />
    <template v-else-if="heroTrack">
      <section class="discovery-hero">
        <img :src="coverFor(heroTrack)" :alt="`${heroTrack.title} 封面`">
        <div class="discovery-hero-overlay" aria-hidden="true"></div>
        <div class="discovery-hero-copy">
          <span class="hero-label">今日新声</span>
          <h2>{{ heroTrack.title }}</h2>
          <p>{{ heroTrack.artistStageName }}</p>
          <button type="button" class="hero-play" :aria-label="`播放 ${heroTrack.title}`" @click="play(heroTrack)">
            <Play :size="23" fill="currentColor" aria-hidden="true" />
            <span>立即收听</span>
          </button>
        </div>
      </section>

      <section class="content-section recommendations-section">
        <div class="section-heading"><h2>热门推荐</h2><router-link to="/search?sort=POPULAR">查看全部</router-link></div>
        <div class="featured-grid">
          <article v-for="track in recommendations" :key="track.id" class="featured-track">
            <button class="cover-button" type="button" :aria-label="`播放 ${track.title}`" @click="play(track)">
              <img :src="coverFor(track)" :alt="`${track.title} 封面`">
              <span><Play :size="20" fill="currentColor" aria-hidden="true" /></span>
            </button>
            <strong>{{ track.title }}</strong><small>{{ track.artistStageName }}</small>
          </article>
        </div>
      </section>

      <section v-if="artistHighlights.length" class="content-section artist-highlights">
        <div class="section-heading"><h2>热门艺人</h2><ArrowRight :size="18" aria-hidden="true" /></div>
        <div class="artist-rail">
          <router-link v-for="item in artistHighlights" :key="item.artistUserId" :to="`/artists/${item.artistUserId}`" class="artist-chip">
            <img :src="coverFor(item)" :alt="`${item.artistStageName} 头像`">
            <span>{{ item.artistStageName }}</span>
          </router-link>
        </div>
      </section>

      <section class="content-section ranking-section">
        <div class="section-heading"><h2>听歌排行</h2><span>按播放量排序</span></div>
        <div class="track-list-section"><TrackRow v-for="(track, index) in rankingTracks" :key="track.id" :track="track" :index="index" @play="play" /></div>
      </section>
    </template>
    <p v-else class="empty-state">暂时没有可播放的音乐。</p>
  </div>
</template>

<script>
import { ArrowRight, Play, Search } from 'lucide-vue';
import { mediaObjectUrl } from '@music-dreamer/shared';
import { api } from '../api.js';
import { loadDiscovery } from '../catalog/discovery.js';
import StateMessage from '../components/StateMessage.vue';
import TrackRow from '../components/TrackRow.vue';
import { problemMessage } from '../ui/problem-message.js';

export default {
  name: 'DiscoverView',
  components: { ArrowRight, Play, Search, StateMessage, TrackRow },
  data: () => ({ state: 'loading', stateMessage: '正在加载音乐…', discovery: null }),
  computed: {
    heroTrack() { return this.discovery && this.discovery.latest.tracks[0]; },
    recommendations() { return this.discovery ? this.discovery.popular.tracks.slice(0, 4) : []; },
    rankingTracks() { return this.discovery ? this.discovery.popular.tracks.slice(0, 8) : []; },
    artistHighlights() {
      if (!this.discovery) return [];
      const tracks = [...this.discovery.latest.tracks, ...this.discovery.popular.tracks];
      return tracks.filter((track, index) => tracks.findIndex((candidate) => candidate.artistUserId === track.artistUserId) === index).slice(0, 6);
    }
  },
  async mounted() {
    try { this.discovery = await loadDiscovery(api); this.state = 'ready'; }
    catch (problem) { this.state = 'error'; this.stateMessage = problemMessage(problem); }
  },
  methods: {
    coverFor(track) { return track.coverMediaObjectId ? mediaObjectUrl(track.coverMediaObjectId) : '/assets/studio-piano.jpg'; },
    play(track) { this.$store.dispatch('player/playTrack', track); }
  }
};
</script>
