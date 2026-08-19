<template>
  <div class="page-shell"><header class="page-header compact"><div><h1>搜索</h1><p class="page-subtitle">按歌曲或歌手查找。</p></div></header><form class="search-form" @submit.prevent="search"><input v-model.trim="keyword" autofocus placeholder="搜索歌曲或歌手" aria-label="搜索歌曲或歌手"><button class="primary-button search-submit" type="submit"><Search :size="18" aria-hidden="true" /><span>搜索</span></button></form><StateMessage v-if="state !== 'ready'" :state="state" :message="message" /><section v-else class="search-results"><div class="section-heading"><h2>{{ keyword ? `“${keyword}”的结果` : '全部歌曲' }}</h2><span>按{{ sort === 'POPULAR' ? '播放量' : '最新发布' }}排序</span></div><TrackRow v-for="(track, index) in tracks" :key="track.id" :track="track" :index="index" @play="selectTrack" /><p v-if="!tracks.length" class="empty-state">暂时没有找到歌曲。</p></section></div>
</template>
<script>
import { Search } from 'lucide-vue';
import { API_PATHS, mapTrackSummary } from '@music-dreamer/shared';
import { api } from '../api.js';
import StateMessage from '../components/StateMessage.vue';
import TrackRow from '../components/TrackRow.vue';
export default { name: 'SearchView', components: { Search, StateMessage, TrackRow }, data: () => ({ keyword: '', sort: 'LATEST', state: 'ready', message: '', tracks: [] }), async mounted() { this.keyword = String(this.$route.query.keyword || ''); this.sort = this.$route.query.sort === 'POPULAR' ? 'POPULAR' : 'LATEST'; await this.search(); }, methods: { async search() { this.state = 'loading'; try { const result = await api.get(API_PATHS.catalog.search({ keyword: this.keyword, sort: this.sort })); this.tracks = result.data.map(mapTrackSummary); this.state = 'ready'; } catch (problem) { this.message = problem.detail || '搜索失败，请稍后重试。'; this.state = 'error'; } }, selectTrack(track) { this.$store.dispatch('player/playTrack', track); } } };
</script>
