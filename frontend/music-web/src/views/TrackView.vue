<template>
  <div class="page-shell track-page">
    <StateMessage v-if="state !== 'ready'" :state="state" :message="message" />
    <template v-else>
      <header class="track-mobile-header"><button type="button" aria-label="返回" title="返回" @click="$router.back()"><ChevronDown :size="21" aria-hidden="true" /></button><span>正在播放</span><span aria-hidden="true"></span></header>
      <section class="track-detail-layout">
        <div class="track-summary">
          <img class="detail-cover" :src="coverFor(track)" :alt="`${track.title} 封面`">
          <p class="status-text">{{ track.status === 'PUBLISHED' ? '已发布' : track.status }}</p>
          <h1>{{ track.title }}</h1>
          <router-link :to="`/artists/${track.artistUserId}`" class="artist-link">{{ track.artistStageName }}</router-link>
          <p class="track-meta">{{ formatDuration(track.durationMs) }} · 播放 {{ track.playCount || 0 }} 次</p>
          <div class="detail-actions">
            <button class="primary-button action-button" type="button" @click="togglePlayback"><Pause v-if="isCurrentTrack && isPlaying" :size="18" fill="currentColor" aria-hidden="true" /><Play v-else :size="18" fill="currentColor" aria-hidden="true" /><span>{{ isCurrentTrack && isPlaying ? '暂停' : '播放' }}</span></button>
            <button class="secondary-button favorite-button" type="button" @click="toggleFavorite"><Heart :size="18" :fill="favorite ? 'currentColor' : 'none'" aria-hidden="true" /><span>{{ favorite ? '取消收藏' : '收藏' }}</span></button>
          </div>

          <div class="mobile-player-controls">
            <input type="range" min="0" :max="progressMax" :value="progressValue" :disabled="!isCurrentTrack" aria-label="播放进度" @input="seek">
            <div><time>{{ formatDuration(progressValue * 1000) }}</time><time>{{ formatDuration(progressMax * 1000) }}</time></div>
            <button type="button" :aria-label="isCurrentTrack && isPlaying ? '暂停' : '播放'" @click="togglePlayback"><Pause v-if="isCurrentTrack && isPlaying" :size="28" fill="currentColor" aria-hidden="true" /><Play v-else :size="28" fill="currentColor" aria-hidden="true" /></button>
          </div>
        </div>

        <section class="lyrics-section" aria-label="歌词">
          <div class="section-heading"><h2>歌词</h2><button v-if="hasLyrics && lyricsLoaded" type="button" @click="loadLyrics">刷新</button></div>
          <p v-if="!hasLyrics" class="lyrics-empty">暂无歌词</p>
          <button v-else-if="!lyricsLoaded" class="lyrics-load" type="button" @click="loadLyrics"><FileText :size="18" aria-hidden="true" /><span>加载歌词</span></button>
          <p v-else-if="!lyrics.length" class="lyrics-empty">暂无歌词</p>
          <div v-else class="lyrics-lines">
            <p v-for="line in lyrics" :key="`${line.timeMs}-${line.text}`" :class="{ active: activeLyric && activeLyric.timeMs === line.timeMs && activeLyric.text === line.text }">{{ line.text }}</p>
          </div>
        </section>

        <aside class="up-next-panel">
          <div class="section-heading"><h2>接下来播放</h2><router-link to="/search?sort=POPULAR">查看全部</router-link></div>
          <TrackRow v-for="(item, index) in suggestions" :key="item.id" :track="item" :index="index" @play="playTrack" />
          <p v-if="!suggestions.length" class="lyrics-empty">暂无待播歌曲</p>
        </aside>
      </section>
    </template>
  </div>
</template>
<script>
import { ChevronDown, FileText, Heart, Pause, Play } from 'lucide-vue';
import { API_PATHS, mapTrackSummary, mediaObjectUrl } from '@music-dreamer/shared';
import { api } from '../api.js';
import StateMessage from '../components/StateMessage.vue';
import TrackRow from '../components/TrackRow.vue';
import { lyricsAvailable } from '../player/media.js';
import { lyricAt, parseLrc } from '../player/lrc.js';
import { problemMessage } from '../ui/problem-message.js';
import { library } from '../library/client.js';

export default {
  name: 'TrackView',
  components: { ChevronDown, FileText, Heart, Pause, Play, StateMessage, TrackRow },
  data: () => ({ state: 'loading', message: '正在加载歌曲…', track: null, favorite: false, lyrics: [], lyricsLoaded: false, suggestions: [] }),
  computed: {
    hasLyrics() { return lyricsAvailable(this.track); },
    activeLyric() { return lyricAt(this.lyrics, this.$store.state.player.currentTime * 1000); },
    isCurrentTrack() { return this.$store.state.player.track && this.$store.state.player.track.id === this.track.id; },
    isPlaying() { return this.$store.state.player.playing; },
    progressValue() { return this.isCurrentTrack ? this.$store.state.player.currentTime : 0; },
    progressMax() { return this.isCurrentTrack && this.$store.state.player.duration ? this.$store.state.player.duration : Math.max(1, (this.track.durationMs || 0) / 1000); }
  },
  async mounted() {
    try {
      const result = await api.get(API_PATHS.catalog.track(Number(this.$route.params.trackId)));
      this.track = mapTrackSummary(result.data);
      this.favorite = (await library.favorites()).some((item) => item.trackId === this.track.id);
      this.state = 'ready';
      this.loadSuggestions();
    } catch (problem) {
      this.state = 'error';
      this.message = problemMessage(problem);
    }
  },
  methods: {
    coverFor(track) { if (track.coverMediaObjectId) return mediaObjectUrl(track.coverMediaObjectId); return track.id % 2 ? '/assets/studio-piano.jpg' : '/assets/studio-computer.jpg'; },
    formatDuration(ms) { const total = Math.round((ms || 0) / 1000); return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`; },
    playTrack(track) { this.$store.dispatch('player/playTrack', track); },
    togglePlayback() { if (this.isCurrentTrack) this.$store.dispatch('player/toggle'); else this.playTrack(this.track); },
    seek(event) { if (this.isCurrentTrack) this.$store.dispatch('player/seek', Number(event.target.value)); },
    async loadSuggestions() {
      try {
        const result = await api.get(API_PATHS.catalog.search({ keyword: '', sort: 'POPULAR' }));
        this.suggestions = result.data.map(mapTrackSummary).filter((item) => item.id !== this.track.id).slice(0, 4);
      } catch (_problem) {
        this.suggestions = [];
      }
    },
    async toggleFavorite() {
      try {
        if (this.favorite) await library.removeFavorite(this.track.id); else await library.addFavorite(this.track.id);
        this.favorite = !this.favorite;
      } catch (problem) {
        this.message = problemMessage(problem);
      }
    },
    async loadLyrics() {
      try {
        const result = await api.get(mediaObjectUrl(this.track.lyricsMediaObjectId), { mediaRequest: true, responseType: 'text' });
        this.lyrics = parseLrc(result.data);
        this.lyricsLoaded = true;
      } catch (problem) {
        this.message = problemMessage(problem);
      }
    }
  }
};
</script>
