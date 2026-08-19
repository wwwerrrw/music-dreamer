<template>
  <button class="track-row" type="button" @click="$emit('play', track)">
    <span class="track-index">{{ String(index + 1).padStart(2, '0') }}</span>
    <span class="track-copy"><strong>{{ track.title }}</strong><small>{{ track.artistStageName }}</small></span>
    <span class="track-duration">{{ formatDuration(track.durationMs) }}</span>
    <Play :size="18" :stroke-width="2" aria-hidden="true" />
  </button>
</template>

<script>
import { Play } from 'lucide-vue';

export default {
  name: 'TrackRow',
  components: { Play },
  props: { track: { type: Object, required: true }, index: { type: Number, default: 0 } },
  methods: {
    formatDuration(milliseconds) {
      const seconds = Math.max(0, Math.round((milliseconds || 0) / 1000));
      return `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
    }
  }
};
</script>
