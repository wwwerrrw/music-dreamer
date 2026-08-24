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
        <span>{{ tracks.length }} 首歌曲<span v-if="artists.length"> · {{ artists.length }} 位歌手</span></span>
      </div>

      <section v-if="artists.length" class="search-artist-results" aria-labelledby="search-artists-heading">
        <div class="section-heading"><h2 id="search-artists-heading">歌手</h2><span>进入主页</span></div>
        <div class="artist-result-list">
          <router-link v-for="artist in artists" :key="artist.userId" class="artist-result-card" :to="`/artists/${artist.userId}`">
            <span class="artist-result-avatar"><Mic2 :size="19" aria-hidden="true" /></span>
            <span><strong>{{ artist.stageName }}</strong><small>{{ artist.biography || '认证音乐人' }}</small></span>
            <ChevronRight :size="17" aria-hidden="true" />
          </router-link>
        </div>
      </section>

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
      <p v-if="!tracks.length && !artists.length" class="empty-state">暂时没有找到歌曲或歌手。</p>
    </section>
  </div>
</template>
<script>
import { ArrowLeft, ChevronRight, Mic2, Search, X } from 'lucide-vue';
import { mediaObjectUrl } from '@music-dreamer/shared';
import { api } from '../api.js';
import { loadSearchResults } from '../catalog/search.js';
import StateMessage from '../components/StateMessage.vue';
import TrackRow from '../components/TrackRow.vue';

export default {
  name: 'SearchView',
  components: { ArrowLeft, ChevronRight, Mic2, Search, StateMessage, TrackRow, X },
  data: () => ({ keyword: '', sort: 'LATEST', state: 'ready', message: '', tracks: [], artists: [] }),
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
      try {
        const result = await loadSearchResults(api, { keyword: this.keyword, sort: this.sort });
        this.tracks = result.tracks;
        this.artists = result.artists;
        this.state = 'ready';
      } catch (problem) {
        this.message = problem.detail || '搜索失败，请稍后重试。';
        this.state = 'error';
      }
    },
    clearSearch() { this.keyword = ''; this.search(); },
    setSort(sort) { this.sort = sort; this.search(); },
    coverFor(track) { return track.coverMediaObjectId ? mediaObjectUrl(track.coverMediaObjectId) : '/assets/studio-piano.jpg'; },
    selectTrack(track) { this.$store.dispatch('player/playTrack', { track, queue: this.tracks }); }
  }
};
</script>
