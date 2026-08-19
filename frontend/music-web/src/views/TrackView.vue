<template>
  <div class="page-shell">
    <StateMessage v-if="state !== 'ready'" :state="state" :message="message" />
    <template v-else>
      <section class="track-detail">
        <img class="detail-cover" :src="coverFor(track)" :alt="`${track.title} 封面`">
        <div class="detail-copy">
          <p class="status-text">{{ track.status === 'PUBLISHED' ? '可播放' : track.status }}</p>
          <h1>{{ track.title }}</h1>
          <router-link :to="`/artists/${track.artistUserId}`" class="artist-link">{{ track.artistStageName }}</router-link>
          <p class="track-meta">{{ formatDuration(track.durationMs) }} · 播放 {{ track.playCount || 0 }} 次</p>
          <div class="detail-actions"><button class="primary-button action-button" type="button" @click="play"><Play :size="18" fill="currentColor" aria-hidden="true" /><span>播放</span></button><button class="secondary-button" type="button" @click="toggleFavorite"><Heart :size="18" :fill="favorite ? 'currentColor' : 'none'" aria-hidden="true" /><span>{{ favorite ? '取消收藏' : '收藏' }}</span></button><button v-if="hasLyrics" class="secondary-button" type="button" @click="loadLyrics"><FileText :size="18" aria-hidden="true" /><span>{{ lyricsLoaded ? '刷新歌词' : '查看歌词' }}</span></button></div>
        </div>
      </section>
      <section v-if="lyricsLoaded" class="lyrics-section" aria-label="歌词"><h2>歌词</h2><p v-for="line in lyrics" :key="`${line.timeMs}-${line.text}`" :class="{ active: activeLyric && activeLyric.timeMs === line.timeMs && activeLyric.text === line.text }">{{ line.text }}</p></section>
    </template>
  </div>
</template>
<script>
import { FileText, Heart, Play } from 'lucide-vue';
import { API_PATHS, mapTrackSummary, mediaObjectUrl } from '@music-dreamer/shared';
import { api } from '../api.js';
import StateMessage from '../components/StateMessage.vue';
import { lyricsAvailable } from '../player/media.js';
import { lyricAt, parseLrc } from '../player/lrc.js';
import { problemMessage } from '../ui/problem-message.js';
import { library } from '../library/client.js';
export default { name: 'TrackView', components: { FileText, Heart, Play, StateMessage }, data: () => ({ state: 'loading', message: '正在加载歌曲…', track: null, favorite: false, lyrics: [], lyricsLoaded: false }), computed: { hasLyrics() { return lyricsAvailable(this.track); }, activeLyric() { return lyricAt(this.lyrics, this.$store.state.player.currentTime * 1000); } }, async mounted() { try { const result = await api.get(API_PATHS.catalog.track(Number(this.$route.params.trackId))); this.track = mapTrackSummary(result.data); this.favorite = (await library.favorites()).some((item) => item.trackId === this.track.id); this.state = 'ready'; } catch (problem) { this.state = 'error'; this.message = problemMessage(problem); } }, methods: { coverFor(track) { if (track.coverMediaObjectId) return mediaObjectUrl(track.coverMediaObjectId); return track.id % 2 ? '/assets/studio-piano.jpg' : '/assets/studio-computer.jpg'; }, formatDuration(ms) { const total = Math.round((ms || 0) / 1000); return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`; }, play() { this.$store.dispatch('player/playTrack', this.track); }, async toggleFavorite() { try { if (this.favorite) await library.removeFavorite(this.track.id); else await library.addFavorite(this.track.id); this.favorite = !this.favorite; } catch (problem) { this.message = problemMessage(problem); } }, async loadLyrics() { try { const result = await api.get(mediaObjectUrl(this.track.lyricsMediaObjectId), { mediaRequest: true, responseType: 'text' }); this.lyrics = parseLrc(result.data); this.lyricsLoaded = true; } catch (problem) { this.message = problemMessage(problem); } } } };
</script>
