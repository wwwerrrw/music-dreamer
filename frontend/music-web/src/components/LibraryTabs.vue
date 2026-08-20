<template>
  <nav class="library-tabs" aria-label="我的音乐分类">
    <router-link to="/library/favorites"><Heart :size="16" aria-hidden="true" /><span>收藏歌曲</span></router-link>
    <router-link to="/library/history"><History :size="16" aria-hidden="true" /><span>最近播放</span></router-link>
    <router-link to="/library/playlists"><ListMusic :size="16" aria-hidden="true" /><span>我的歌单</span></router-link>
  </nav>
</template>

<script>
import { Heart, History, ListMusic } from 'lucide-vue';

export default {
  name: 'LibraryTabs',
  components: { Heart, History, ListMusic }
};
</script>
