<template>
  <div class="page-shell account-profile-page">
    <StateMessage v-if="state !== 'ready'" :state="state" :message="message" />
    <template v-else>
      <header class="account-profile-header">
        <span class="account-profile-avatar"><UserRound :size="34" aria-hidden="true" /></span>
        <div>
          <span class="account-profile-eyebrow">{{ roleLabel }}</span>
          <h1>{{ account.username }}</h1>
          <p>{{ account.email }}</p>
        </div>
        <span class="account-profile-id">用户 #{{ account.id }}</span>
      </header>

      <section class="account-profile-stats" aria-label="我的音乐统计">
        <router-link to="/library/favorites">
          <Heart :size="19" aria-hidden="true" />
          <strong>{{ counts.favorites }}</strong>
          <span>收藏歌曲</span>
        </router-link>
        <router-link to="/library/playlists">
          <ListMusic :size="19" aria-hidden="true" />
          <strong>{{ counts.playlists }}</strong>
          <span>自建歌单</span>
        </router-link>
        <router-link to="/library/history">
          <History :size="19" aria-hidden="true" />
          <strong>{{ counts.history }}</strong>
          <span>最近播放</span>
        </router-link>
      </section>

      <section class="account-profile-links" aria-labelledby="profile-links-heading">
        <div class="section-heading"><h2 id="profile-links-heading">快捷入口</h2><span>管理我的音乐</span></div>
        <router-link to="/library/playlists"><span><ListMusic :size="18" aria-hidden="true" />我的歌单</span><ChevronRight :size="18" aria-hidden="true" /></router-link>
        <router-link to="/messages"><span><Mail :size="18" aria-hidden="true" />消息中心</span><ChevronRight :size="18" aria-hidden="true" /></router-link>
        <template v-if="isSinger">
          <router-link :to="`/artists/${account.id}`"><span><Mic2 :size="18" aria-hidden="true" />公开歌手主页</span><ChevronRight :size="18" aria-hidden="true" /></router-link>
          <router-link to="/artist/studio"><span><Upload :size="18" aria-hidden="true" />歌手工作台</span><ChevronRight :size="18" aria-hidden="true" /></router-link>
        </template>
      </section>
    </template>
  </div>
</template>

<script>
import { ChevronRight, Heart, History, ListMusic, Mail, Mic2, Upload, UserRound } from 'lucide-vue';
import { api } from '../api.js';
import StateMessage from '../components/StateMessage.vue';
import { library } from '../library/client.js';
import { loadProfile } from '../profile/load-profile.js';
import { problemMessage } from '../ui/problem-message.js';

export default {
  name: 'ProfileView',
  components: { ChevronRight, Heart, History, ListMusic, Mail, Mic2, StateMessage, Upload, UserRound },
  data: () => ({
    state: 'loading',
    message: '正在加载个人主页…',
    account: null,
    counts: { favorites: 0, playlists: 0, history: 0 }
  }),
  computed: {
    isSinger() { return this.account?.role === 'SINGER'; },
    roleLabel() { return this.isSinger ? '歌手账号' : '普通用户'; }
  },
  async mounted() {
    try {
      const profile = await loadProfile({ api, library });
      this.account = profile.account;
      this.counts = profile.counts;
      this.state = 'ready';
    } catch (problem) {
      this.state = 'error';
      this.message = problemMessage(problem);
    }
  }
};
</script>
