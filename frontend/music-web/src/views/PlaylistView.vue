<template>
  <div class="page-shell playlist-detail-page">
    <StateMessage v-if="state !== 'ready'" :state="state" :message="error || '正在加载歌单…'" />
    <template v-else-if="playlist">
      <header class="playlist-detail-header">
        <router-link class="mobile-back-button" to="/library/playlists" aria-label="返回我的歌单" title="返回我的歌单"><ArrowLeft :size="20" aria-hidden="true" /></router-link>
        <span class="playlist-detail-art"><ListMusic :size="40" aria-hidden="true" /></span>
        <div><p>我的歌单</p><h1>{{ playlist.playlist.name }}</h1><span>{{ playlist.playlist.description || '私人歌单' }} · {{ trackDetails.length }} 首歌曲</span></div>
      </header>
      <form class="playlist-add-form" @submit.prevent="add">
        <label for="playlist-track-id">添加歌曲</label>
        <input id="playlist-track-id" v-model.number="trackId" type="number" min="1" placeholder="输入歌曲 ID" required>
        <button class="primary-button" type="submit"><Plus :size="18" aria-hidden="true" /><span>添加</span></button>
      </form>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <section class="playlist-tracks">
        <div v-for="(track, index) in trackDetails" :key="track.id" class="playlist-track-row">
          <TrackRow :track="track" :index="index" @play="play" />
          <button class="icon-button light" type="button" :aria-label="`将 ${track.title} 移出歌单`" title="移出歌单" @click="remove(track.id)"><Trash2 :size="18" aria-hidden="true" /></button>
        </div>
        <p v-if="!trackDetails.length" class="empty-state">歌单里还没有歌曲。</p>
      </section>
    </template>
  </div>
</template>
<script>
import { ArrowLeft, ListMusic, Plus, Trash2 } from 'lucide-vue';
import { API_PATHS, mapTrackSummary } from '@music-dreamer/shared';
import { api } from '../api.js';
import StateMessage from '../components/StateMessage.vue';
import TrackRow from '../components/TrackRow.vue';
import { library } from '../library/client.js';
import { problemMessage } from '../ui/problem-message.js';

export default {
  name: 'PlaylistView',
  components: { ArrowLeft, ListMusic, Plus, StateMessage, TrackRow, Trash2 },
  data: () => ({ state: 'loading', playlist: null, trackDetails: [], trackId: null, error: '' }),
  async mounted() { await this.load(); },
  methods: {
    async load() {
      this.state = 'loading';
      try {
        this.playlist = await library.playlist(Number(this.$route.params.playlistId));
        this.trackDetails = await Promise.all(this.playlist.tracks.map(async (item) => mapTrackSummary((await api.get(API_PATHS.catalog.track(item.trackId))).data)));
        this.state = 'ready';
      } catch (problem) {
        this.state = 'error';
        this.error = problemMessage(problem);
      }
    },
    async add() {
      try {
        await library.addTrack(this.playlist.playlist.id, { trackId: Number(this.trackId), sortOrder: this.playlist.tracks.length });
        await this.load();
        this.trackId = null;
      } catch (problem) {
        this.error = problemMessage(problem);
      }
    },
    async remove(trackId) {
      try {
        await library.removeTrack(this.playlist.playlist.id, trackId);
        await this.load();
      } catch (problem) {
        this.error = problemMessage(problem);
      }
    },
    play(track) { this.$store.dispatch('player/playTrack', track); }
  }
};
</script>
