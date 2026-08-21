<template>
  <div class="page-shell artist-studio-page">
    <header class="page-header compact"><div><span class="studio-breadcrumb">上传歌曲 · 歌手工作台</span><h1>上传歌曲</h1><p class="page-subtitle">整理媒体与歌曲资料，保存草稿后提交审核。</p></div><router-link class="secondary-button" to="/artist/profile"><UserRound :size="18" aria-hidden="true" /><span>歌手资料</span></router-link></header>
    <div class="studio-layout">
      <form class="editor-form studio-form" @submit.prevent="saveDraft">
        <section class="studio-section">
          <div class="form-section-heading"><span>01</span><div><h2>媒体文件</h2><p>上传音频、封面与可选歌词。</p></div></div>
          <div class="studio-upload-grid">
            <label class="file-dropzone primary-dropzone"><UploadCloud :size="28" aria-hidden="true" /><strong>音频文件</strong><span>{{ form.audio ? form.audio.name : draft ? '沿用现有音频' : '选择音频文件' }}</span><input ref="audio" type="file" accept="audio/*" :required="!draft" :disabled="Boolean(draft)" @change="pick('audio', $event)"><small>{{ draft ? '音频保存后不可替换。' : '必填，由 Media 服务校验内容。' }}</small></label>
            <label class="file-dropzone"><Image :size="24" aria-hidden="true" /><strong>封面图片</strong><span>{{ form.cover ? form.cover.name : draft ? '沿用现有封面，可重新选择' : '选择图片' }}</span><input ref="cover" type="file" accept="image/*" @change="pick('cover', $event)"><small>可选。</small></label>
            <label class="file-dropzone"><FileText :size="24" aria-hidden="true" /><strong>歌词文件</strong><span>{{ form.lyrics ? form.lyrics.name : draft ? '沿用现有歌词，可重新选择' : '选择 LRC' }}</span><input ref="lyrics" type="file" accept=".lrc,text/plain" @change="pick('lyrics', $event)"><small>可选，支持 LRC。</small></label>
          </div>
        </section>

        <section class="studio-section">
          <div class="form-section-heading"><span>02</span><div><h2>基本信息</h2><p>填写审核与展示所需的歌曲资料。</p></div></div>
          <div class="studio-field-grid"><label>歌曲名称<input v-model.trim="form.title" maxlength="200" placeholder="输入歌曲名称" required></label><label>时长（秒）<input v-model.number="form.durationSeconds" type="number" min="1" required></label></div>
        </section>

        <section class="studio-section">
          <div class="form-section-heading"><span>03</span><div><h2>发布设置</h2><p>确认当前稿件状态后提交。</p></div></div>
          <div class="publish-status"><span :class="{ active: !draft || draft.status === 'DRAFT' }">草稿</span><ArrowRight :size="17" aria-hidden="true" /><span :class="{ active: draft && draft.status !== 'DRAFT' }">审核中</span></div>
        </section>

        <p v-if="message" :class="success ? 'form-success' : 'form-error'" role="status">{{ message }}</p>
        <div class="studio-actions"><button class="secondary-button" type="submit" :disabled="submitting"><Save :size="18" aria-hidden="true" /><span>{{ submitting ? '保存中…' : '保存草稿' }}</span></button><button class="primary-button" type="button" :disabled="!draft || draft.status !== 'DRAFT' || submitting" @click="submitDraft"><Send :size="18" aria-hidden="true" /><span>提交审核</span></button></div>
      </form>

      <aside class="studio-aside">
        <div class="section-heading"><h2>草稿</h2><span>{{ draft ? '1 条' : '0 条' }}</span></div>
        <article v-if="draft" class="studio-draft-card"><span class="status-text">{{ statusLabel }}</span><h3>{{ draft.title }}</h3><p>草稿 #{{ draft.id }}</p><small>{{ form.durationSeconds }} 秒</small></article>
        <div v-else class="studio-empty-draft"><Music2 :size="28" aria-hidden="true" /><p>保存后，当前草稿会显示在这里。</p></div>
      </aside>
    </div>
  </div>
</template>
<script>
import { ArrowRight, FileText, Image, Music2, Save, Send, UploadCloud, UserRound } from 'lucide-vue';
import { artistStudio } from '../artist/client.js';
import { draftStatusLabel, preserveStudioForm } from '../artist/studio-model.js';
import { createSingleFlight } from '../ui/async-control.js';
import { problemMessage } from '../ui/problem-message.js';

export default {
  name: 'ArtistStudioView',
  components: { ArrowRight, FileText, Image, Music2, Save, Send, UploadCloud, UserRound },
  data: () => ({ form: { title: '', durationSeconds: 180, audio: null, cover: null, lyrics: null }, draft: null, submitting: false, success: false, message: '', submitOnce: createSingleFlight() }),
  computed: {
    statusLabel() { return draftStatusLabel(this.draft && this.draft.status); }
  },
  methods: {
    pick(field, event) { this.form[field] = event.target.files && event.target.files[0] ? event.target.files[0] : null; },
    payload() {
      return {
        title: this.form.title,
        durationMs: Math.round(Number(this.form.durationSeconds) * 1000),
        mediaObjectId: this.draft.mediaObjectId,
        coverMediaObjectId: this.draft.coverMediaObjectId ?? null,
        lyricsMediaObjectId: this.draft.lyricsMediaObjectId ?? null,
        audio: this.form.audio,
        cover: this.form.cover,
        lyrics: this.form.lyrics
      };
    },
    clearSelectedFiles() {
      for (const field of ['audio', 'cover', 'lyrics']) {
        this.form[field] = null;
        if (this.$refs[field]) this.$refs[field].value = '';
      }
    },
    async saveDraft() {
      this.submitting = true;
      this.message = '';
      try {
        if (this.draft) this.draft = await artistStudio.updateDraft(this.draft.id, this.payload());
        else this.draft = await artistStudio.createDraft({ title: this.form.title, durationMs: Math.round(Number(this.form.durationSeconds) * 1000), audio: this.form.audio, cover: this.form.cover, lyrics: this.form.lyrics });
        this.clearSelectedFiles();
        this.success = true;
        this.message = '草稿已保存。';
      } catch (problem) {
        this.form = preserveStudioForm(this.form, problem);
        this.success = false;
        this.message = problemMessage(problem);
      } finally {
        this.submitting = false;
      }
    },
    async submitDraft() {
      return this.submitOnce(async () => {
        this.submitting = true;
        this.message = '';
        try {
          this.draft = await artistStudio.submit(this.draft.id);
          this.success = true;
          this.message = '歌曲已提交审核。';
        } catch (problem) {
          this.success = false;
          this.message = problemMessage(problem);
        } finally {
          this.submitting = false;
        }
      });
    }
  }
};
</script>
