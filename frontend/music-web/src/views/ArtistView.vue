<template>
  <div class="page-shell artist-page">
    <StateMessage v-if="state !== 'ready'" :state="state" :message="message" />
    <template v-else>
      <header class="artist-header">
        <div class="artist-cover-backdrop"><img v-if="artistCover" :src="artistCover" alt="" aria-hidden="true"></div>
        <div class="artist-header-content">
          <div class="artist-avatar">
            <img v-if="artistCover" :src="artistCover" :alt="`${artist.stageName} 头像`">
            <Mic2 v-else :size="40" aria-hidden="true" />
          </div>
          <div class="artist-copy">
            <span>认证音乐人</span>
            <h1>{{ artist.stageName }}</h1>
            <p>{{ tracks.length }} 首已发布作品</p>
            <button v-if="tracks.length" class="primary-button" type="button" @click="play(tracks[0])"><Play :size="18" fill="currentColor" aria-hidden="true" /><span>播放全部</span></button>
          </div>
        </div>
      </header>

      <div class="artist-content-layout">
        <section class="artist-tracks">
          <div class="section-heading"><h2>热门单曲</h2><span>{{ tracks.length }} 首</span></div>
          <TrackRow v-for="(track, index) in tracks" :key="track.id" :track="track" :index="index" @play="play" />
          <p v-if="!tracks.length" class="empty-state">暂时没有已发布歌曲。</p>
        </section>
        <aside class="artist-about">
          <h2>关于</h2>
          <p>{{ artist.biography || '这位歌手还没有填写简介。' }}</p>
          <dl><div><dt>艺名</dt><dd>{{ artist.stageName }}</dd></div><div><dt>作品</dt><dd>{{ tracks.length }} 首</dd></div></dl>
        </aside>
      </div>
    </template>
  </div>
</template>
<script>
import { Mic2, Play } from 'lucide-vue';
import { API_PATHS, mapTrackSummary, mediaObjectUrl } from '@music-dreamer/shared';
import { api } from '../api.js';
import StateMessage from '../components/StateMessage.vue';
import TrackRow from '../components/TrackRow.vue';
import { problemMessage } from '../ui/problem-message.js';

export default {
  name: 'ArtistView',
  components: { Mic2, Play, StateMessage, TrackRow },
  data: () => ({ state: 'loading', message: '正在加载歌手…', artist: null, tracks: [] }),
  computed: {
    artistCover() {
      const track = this.tracks.find((item) => item.coverMediaObjectId);
      return track ? mediaObjectUrl(track.coverMediaObjectId) : '';
    }
  },
  async mounted() {
    try {
      const userId = Number(this.$route.params.userId);
      const [artist, tracks] = await Promise.all([
        api.get(API_PATHS.catalog.artist(userId)),
        api.get(API_PATHS.catalog.artistTracks(userId))
      ]);
      this.artist = artist.data;
      this.tracks = tracks.data.map(mapTrackSummary);
      this.state = 'ready';
    } catch (problem) {
      this.state = 'error';
      this.message = problemMessage(problem);
    }
  },
  methods: {
    play(track) { this.$store.dispatch('player/playTrack', { track, queue: this.tracks }); }
  }
};
</script>
