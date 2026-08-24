<template>
  <section v-if="track" class="player-bar" aria-label="播放器">
    <div class="player-track">
      <router-link
        class="player-cover-link"
        :to="trackDetailLocation(track)"
        :aria-label="`查看${track.title}歌曲详情`"
        title="查看歌曲详情"
      >
        <img :src="coverFor(track)" :alt="`${track.title} 封面`">
      </router-link>
      <div>
        <strong :title="track.title">{{ track.title }}</strong>
        <small>{{ track.artistStageName }}</small>
        <small v-if="playCountError" class="player-sync-error" role="status">{{ playCountError }}</small>
      </div>
    </div>
    <div class="player-center">
      <div class="player-controls">
        <button class="player-icon player-mode" :class="{ 'is-active': playbackMode !== 'ORDER' }" type="button" :aria-label="modeTitle" :title="modeTitle" @click="$emit('cycle-mode')">
          <Shuffle v-if="playbackMode === 'SHUFFLE'" :size="17" aria-hidden="true" />
          <Repeat1 v-else-if="playbackMode === 'REPEAT_ONE'" :size="17" aria-hidden="true" />
          <ListMusic v-else :size="17" aria-hidden="true" />
        </button>
        <button class="player-icon" type="button" aria-label="上一首" title="上一首" @click="$emit('previous')"><SkipBack :size="18" /></button>
        <button class="player-play" type="button" :aria-label="playing ? '暂停' : '播放'" :title="playing ? '暂停' : '播放'" @click="$emit('toggle')"><Pause v-if="playing" :size="20" /><Play v-else :size="20" /></button>
        <button class="player-icon" type="button" aria-label="下一首" title="下一首" @click="$emit('next')"><SkipForward :size="18" /></button>
      </div>
      <div class="player-progress">
        <time>{{ formatTime(currentTime) }}</time>
        <input class="progress-range" type="range" min="0" :max="duration || 1" :value="currentTime" aria-label="播放进度" @input="$emit('seek', Number($event.target.value))">
        <time>{{ formatTime(duration) }}</time>
      </div>
    </div>
    <div class="player-volume">
      <Volume2 :size="18" aria-hidden="true" />
      <input type="range" min="0" max="1" step="0.01" :value="volume" aria-label="音量" @input="$emit('volume', Number($event.target.value))">
    </div>
  </section>
</template>

<script>
import { ListMusic, Pause, Play, Repeat1, Shuffle, SkipBack, SkipForward, Volume2 } from 'lucide-vue';
import { trackDetailLocation } from '../player/media.js';

export default {
  name: 'MiniPlayer',
  components: { ListMusic, Pause, Play, Repeat1, Shuffle, SkipBack, SkipForward, Volume2 },
  props: {
    track: { type: Object, default: null }, playing: Boolean, currentTime: { type: Number, default: 0 },
    duration: { type: Number, default: 0 }, volume: { type: Number, default: 1 }, playCountError: { type: String, default: '' },
    playbackMode: { type: String, default: 'ORDER' }
  },
  computed: {
    modeTitle() {
      const label = { ORDER: '顺序播放', SHUFFLE: '随机播放', REPEAT_ONE: '单曲循环' }[this.playbackMode] || '顺序播放';
      return `播放模式：${label}`;
    }
  },
  methods: {
    trackDetailLocation,
    coverFor(track) { return track.coverMediaObjectId ? `/api/media/objects/${track.coverMediaObjectId}` : '/assets/studio-piano.jpg'; },
    formatTime(seconds) {
      const value = Number.isFinite(seconds) ? Math.max(0, Math.floor(seconds)) : 0;
      return `${Math.floor(value / 60)}:${String(value % 60).padStart(2, '0')}`;
    }
  }
};
</script>
