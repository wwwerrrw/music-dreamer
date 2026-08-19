<template>
  <div class="page-shell"><StateMessage v-if="state !== 'ready'" :state="state" :message="message" /><template v-else><header class="artist-header"><div class="artist-avatar"><Mic2 :size="32" aria-hidden="true" /></div><div><h1>{{ artist.stageName }}</h1><p>{{ artist.biography || '这位歌手还没有填写简介。' }}</p></div></header><section class="content-section"><div class="section-heading"><h2>歌曲</h2><span>{{ tracks.length }} 首</span></div><TrackRow v-for="(track, index) in tracks" :key="track.id" :track="track" :index="index" @play="play" /><p v-if="!tracks.length" class="empty-state">暂时没有已发布歌曲。</p></section></template></div>
</template>
<script>
import { Mic2 } from 'lucide-vue';
import { API_PATHS, mapTrackSummary } from '@music-dreamer/shared';
import { api } from '../api.js';
import StateMessage from '../components/StateMessage.vue';
import TrackRow from '../components/TrackRow.vue';
import { problemMessage } from '../ui/problem-message.js';
export default { name: 'ArtistView', components: { Mic2, StateMessage, TrackRow }, data: () => ({ state: 'loading', message: '正在加载歌手…', artist: null, tracks: [] }), async mounted() { try { const artist = await api.get(API_PATHS.catalog.artist(Number(this.$route.params.userId))); this.artist = artist.data; const tracks = await api.get(API_PATHS.catalog.search({ keyword: this.artist.stageName, sort: 'LATEST' })); this.tracks = tracks.data.map(mapTrackSummary).filter((track) => track.artistUserId === this.artist.userId); this.state = 'ready'; } catch (problem) { this.state = 'error'; this.message = problemMessage(problem); } }, methods: { play(track) { this.$store.dispatch('player/playTrack', track); } } };
</script>
