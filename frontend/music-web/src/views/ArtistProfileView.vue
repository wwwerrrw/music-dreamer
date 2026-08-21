<template>
  <div class="page-shell artist-profile-page">
    <header class="page-header compact"><div><h1>歌手资料</h1><p class="page-subtitle">维护公开展示的艺名和简介。</p></div><router-link class="secondary-button" to="/artist/studio"><Upload :size="17" aria-hidden="true" /><span>上传歌曲</span></router-link></header>
    <StateMessage v-if="state === 'loading'" state="loading" message="正在加载资料…" />
    <div v-else class="profile-layout">
      <form class="editor-form profile-form" @submit.prevent="save">
        <div class="form-section-heading"><span>01</span><div><h2>公开资料</h2><p>这些信息会显示在歌手主页。</p></div></div>
        <label>艺名<input v-model.trim="form.stageName" maxlength="100" placeholder="输入艺名" required></label>
        <label>简介<textarea v-model.trim="form.biography" maxlength="2000" rows="8" placeholder="介绍你的音乐风格与创作经历"></textarea><small>{{ form.biography.length }}/2000</small></label>
        <p v-if="message" :class="success ? 'form-success' : 'form-error'" role="status">{{ message }}</p>
        <button class="primary-button" type="submit" :disabled="saving"><Save :size="18" aria-hidden="true" /><span>{{ saving ? '保存中…' : '保存资料' }}</span></button>
      </form>
      <aside class="profile-preview">
        <span>主页预览</span>
        <div class="profile-preview-avatar"><Mic2 :size="34" aria-hidden="true" /></div>
        <h2>{{ form.stageName || '你的艺名' }}</h2>
        <p>{{ form.biography || '填写简介后将在这里显示。' }}</p>
      </aside>
    </div>
  </div>
</template>
<script>
import { Mic2, Save, Upload } from 'lucide-vue';
import StateMessage from '../components/StateMessage.vue';
import { artistStudio } from '../artist/client.js';
import { problemMessage } from '../ui/problem-message.js';

export default {
  name: 'ArtistProfileView',
  components: { Mic2, Save, StateMessage, Upload },
  data: () => ({ state: 'loading', saving: false, success: false, message: '', form: { stageName: '', biography: '' } }),
  async mounted() {
    try {
      const profile = await artistStudio.profile(this.$store.state.auth.session.userId);
      this.form = { stageName: profile.stageName, biography: profile.biography || '' };
      this.state = 'ready';
    } catch (problem) {
      this.state = 'ready';
      if (problem.status !== 404) this.message = problemMessage(problem);
    }
  },
  methods: {
    async save() {
      this.saving = true;
      this.message = '';
      try {
        const saved = await artistStudio.saveProfile({ ...this.form });
        this.form = { stageName: saved.stageName, biography: saved.biography || '' };
        this.success = true;
        this.message = '歌手资料已保存。';
      } catch (problem) {
        this.success = false;
        this.message = problemMessage(problem);
      } finally {
        this.saving = false;
      }
    }
  }
};
</script>
