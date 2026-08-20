<template>
  <div class="admin-shell">
    <aside class="admin-sidebar" aria-label="后台主导航">
      <router-link class="admin-brand" to="/tracks" aria-label="悦享音乐管理后台" title="悦享音乐"><Music2 :size="22" :stroke-width="2.4" aria-hidden="true" /><span>悦享</span></router-link>
      <nav>
        <router-link to="/tracks" title="歌曲审核"><ListChecks :size="20" aria-hidden="true" /><span>审核</span></router-link>
        <router-link to="/users" title="用户管理"><Users :size="20" aria-hidden="true" /><span>用户</span></router-link>
        <router-link to="/messages" title="消息发布"><Send :size="20" aria-hidden="true" /><span>消息</span></router-link>
      </nav>
      <button class="admin-logout" type="button" title="退出登录" @click="logout"><LogOut :size="20" aria-hidden="true" /><span>退出</span></button>
    </aside>
    <section class="admin-workspace">
      <header class="admin-topbar">
        <router-link to="/tracks" class="admin-topbar-brand"><strong>悦享音乐</strong><span>管理后台</span></router-link>
        <div class="admin-identity"><ShieldCheck :size="17" aria-hidden="true" /><span>管理员</span></div>
      </header>
      <main class="admin-main"><router-view /></main>
    </section>
  </div>
</template>

<script>
import { ListChecks, LogOut, Music2, Send, ShieldCheck, Users } from 'lucide-vue';

export default {
  name: 'AdminShell',
  components: { ListChecks, LogOut, Music2, Send, ShieldCheck, Users },
  methods: {
    async logout() {
      await this.$store.dispatch('logout');
      this.$router.replace('/login');
    }
  }
};
</script>
