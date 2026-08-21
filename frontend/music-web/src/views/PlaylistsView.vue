<template>
  <div class="page-shell library-page">
    <header class="page-header compact"><div><h1>我的音乐</h1><p class="page-subtitle">收藏歌曲、最近播放与自建歌单。</p></div></header>
    <LibraryTabs />
    <p v-if="error" class="form-error" role="alert">{{ error }}</p>
    <StateMessage v-if="state !== 'ready'" :state="state" :message="error || '正在加载歌单…'" />
    <section v-else class="library-section">
      <div class="section-heading"><h2>我的歌单</h2><span>{{ playlists.length }} 个</span></div>
      <div class="playlist-grid">
        <form class="new-playlist-card" @submit.prevent="create">
          <span class="new-playlist-icon"><Plus :size="25" aria-hidden="true" /></span>
          <strong>新建歌单</strong>
          <input v-model.trim="name" maxlength="120" placeholder="输入歌单名称" aria-label="新歌单名称" required>
          <button type="submit">创建</button>
        </form>
        <router-link v-for="item in playlists" :key="item.id" class="playlist-card" :to="`/library/playlists/${item.id}`">
          <span class="playlist-art"><ListMusic :size="34" aria-hidden="true" /></span>
          <strong>{{ item.name }}</strong>
          <small>{{ item.description || '私人歌单' }}</small>
          <ChevronRight :size="18" aria-hidden="true" />
        </router-link>
      </div>
      <p v-if="!playlists.length" class="empty-state">还没有创建歌单。</p>
    </section>
  </div>
</template>
<script>
import { ChevronRight, ListMusic, Plus } from 'lucide-vue';
import LibraryTabs from '../components/LibraryTabs.vue';
import StateMessage from '../components/StateMessage.vue';
import { library } from '../library/client.js';
import { problemMessage } from '../ui/problem-message.js';

export default {
  name: 'PlaylistsView',
  components: { ChevronRight, LibraryTabs, ListMusic, Plus, StateMessage },
  data: () => ({ state: 'loading', playlists: [], name: '', error: '' }),
  async mounted() { await this.load(); },
  methods: {
    async load() {
      this.state = 'loading';
      try {
        this.playlists = await library.playlists();
        this.state = 'ready';
      } catch (problem) {
        this.state = 'error';
        this.error = problemMessage(problem);
      }
    },
    async create() {
      this.error = '';
      try {
        const item = await library.createPlaylist({ name: this.name, description: '', visibility: 'PRIVATE' });
        this.playlists.unshift(item);
        this.name = '';
      } catch (problem) {
        this.error = problemMessage(problem);
      }
    }
  }
};
</script>
