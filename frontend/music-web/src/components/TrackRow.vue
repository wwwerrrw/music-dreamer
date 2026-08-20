<template>
  <button class="track-row" type="button" @click="$emit('play', track)">
    <span class="track-index">{{ index + 1 }}</span>
    <img class="track-cover" :src="coverFor(track)" :alt="`${track.title} 封面`">
    <span class="track-copy">
      <strong :title="track.title">{{ track.title }}</strong>
      <small>{{ track.artistStageName }}</small>
    </span>
    <span v-if="track.playCount !== undefined && track.playCount !== null" class="track-play-count">
      {{ formatPlayCount(track.playCount) }} 次播放
    </span>
    <span class="track-duration">{{ formatDuration(track.durationMs) }}</span>
    <span class="track-play-icon"><Play :size="18" :stroke-width="2.2" fill="currentColor" aria-hidden="true" /></span>
  </button>
</template>

<script>
import { Play } from 'lucide-vue';
import { mediaObjectUrl } from '@music-dreamer/shared';

export default {
  name: 'TrackRow',
  components: { Play },
  props: { track: { type: Object, required: true }, index: { type: Number, default: 0 } },
  methods: {
    coverFor(track) {
      return track.coverMediaObjectId ? mediaObjectUrl(track.coverMediaObjectId) : '/assets/studio-piano.jpg';
    },
    formatDuration(milliseconds) {
      const seconds = Math.max(0, Math.round((milliseconds || 0) / 1000));
      return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`;
    },
    formatPlayCount(value) {
      return new Intl.NumberFormat('zh-CN', { notation: value >= 10000 ? 'compact' : 'standard', maximumFractionDigits: 1 }).format(value || 0);
    }
  }
};
</script>
