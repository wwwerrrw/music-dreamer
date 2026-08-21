<template>
  <div class="page-shell search-page">
    <header class="search-toolbar">
      <button class="mobile-back-button" type="button" aria-label="返回" title="返回" @click="$router.back()"><ArrowLeft :size="20" aria-hidden="true" /></button>
      <form class="search-form" role="search" @submit.prevent="search">
        <Search :size="18" aria-hidden="true" />
        <input v-model.trim="keyword" autofocus placeholder="搜索歌曲或歌手" aria-label="搜索歌曲或歌手">
        <button class="search-clear" :class="{ invisible: !keyword }" type="button" :disabled="!keyword" aria-label="清除搜索" title="清除搜索" @click="clearSearch"><X :size="17" aria-hidden="true" /></button>
        <button class="primary-button search-submit" type="submit"><span>搜索</span></button>
      </form>
    </header>

    <nav class="search-filters" aria-label="搜索排序">
      <button type="button" :class="{ active: sort === 'LATEST' }" @click="setSort('LATEST')">最新发布</button>
      <button type="button" :class="{ active: sort === 'POPULAR' }" @click="setSort('POPULAR')">热门歌曲</button>
    </nav>

    <StateMessage v-if="state !== 'ready'" :state="state" :message="message" />
    <section v-else class="search-results">
      <div class="search-results-heading">
        <div><h1>搜索结果</h1><p>{{ keyword ? `“${keyword}”` : '全部歌曲' }}</p></div>
        <span>{{ tracks.length }} 首歌曲</span>
      </div>

      <div v-if="tracks.length" class="search-result-layout">
        <aside class="best-match">
          <h2>最佳匹配</h2>
          <button type="button" class="best-match-card" @click="selectTrack(bestMatch)">
            <img :src="coverFor(bestMatch)" :alt="`${bestMatch.title} 封面`">
            <strong>{{ bestMatch.title }}</strong>
            <span>{{ bestMatch.artistStageName }}</span>
            <small>播放歌曲</small>
          </button>
        </aside>
        <div class="search-track-results">
          <div class="section-heading"><h2>歌曲</h2><span>按{{ sort === 'POPULAR' ? '播放量' : '最新发布' }}排序</span></div>
          <TrackRow v-for="(track, index) in tracks" :key="track.id" :track="track" :index="index" @play="selectTrack" />
        </div>
      </div>
      <p v-else class="empty-state">暂时没有找到歌曲。</p>
    </section>
  </div>
</template>
<script>
import { ArrowLeft, Search, X } from 'lucide-vue';
import { API_PATHS, mapTrackSummary, mediaObjectUrl } from '@music-dreamer/shared';
import { api } from '../api.js';
import StateMessage from '../components/StateMessage.vue';
import TrackRow from '../components/TrackRow.vue';
import { createLatestOnly } from '../ui/async-control.js';

export default {
  name: 'SearchView',
  components: { ArrowLeft, Search, StateMessage, TrackRow, X },
  data: () => ({ keyword: '', sort: 'LATEST', state: 'ready', message: '', tracks: [], runLatestSearch: createLatestOnly() }),
  computed: {
    bestMatch() { return this.tracks[0] || null; }
  },
  async mounted() {
    this.keyword = String(this.$route.query.keyword || '');
    this.sort = this.$route.query.sort === 'POPULAR' ? 'POPULAR' : 'LATEST';
    await this.search();
  },
  methods: {
    async search() {
      this.state = 'loading';
      this.message = '';
      try {
        const path = API_PATHS.catalog.search({ keyword: this.keyword, sort: this.sort });
        await this.runLatestSearch(
          () => api.get(path),
          (result) => {
            this.tracks = result.data.map(mapTrackSummary);
            this.state = 'ready';
          }
        );
      } catch (problem) {
        this.message = problem.detail || '搜索失败，请稍后重试。';
        this.state = 'error';
      }
    },
    clearSearch() { this.keyword = ''; this.search(); },
    setSort(sort) { this.sort = sort; this.search(); },
    coverFor(track) { return track.coverMediaObjectId ? mediaObjectUrl(track.coverMediaObjectId) : '/assets/studio-piano.jpg'; },
    selectTrack(track) { this.$store.dispatch('player/playTrack', track); }
  }
};
</script>
