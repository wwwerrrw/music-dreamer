<template>
  <div class="admin-page message-publish-page">
    <header class="admin-page-header"><div><span class="admin-eyebrow">平台沟通</span><h1>发布消息</h1><p>编辑并向所有平台用户发布广播消息。</p></div></header>
    <div class="message-publish-layout">
      <el-form ref="form" class="message-editor" :model="form" :rules="rules" label-position="top" @submit.native.prevent="publish">
        <section class="message-editor-section">
          <div class="editor-section-title"><span>01</span><div><h2>发布对象</h2><p>当前广播接口面向所有用户。</p></div></div>
          <div class="audience-value"><Users :size="18" aria-hidden="true" /><span>所有用户</span></div>
        </section>
        <section class="message-editor-section">
          <div class="editor-section-title"><span>02</span><div><h2>消息内容</h2><p>主题简洁明确，正文保留必要信息。</p></div></div>
          <el-form-item label="消息主题" prop="subject"><el-input v-model.trim="form.subject" maxlength="200" show-word-limit placeholder="输入消息主题" /></el-form-item>
          <el-form-item label="正文内容" prop="body"><el-input v-model="form.body" type="textarea" :rows="11" maxlength="2000" show-word-limit placeholder="在这里撰写消息正文" /></el-form-item>
        </section>
        <p v-if="error" class="admin-error" role="alert">{{ error }}</p>
        <p v-if="success" class="admin-success" role="status">{{ success }}</p>
        <div class="message-editor-actions"><span>发布后将立即进入用户消息中心</span><el-button class="publish-button" type="primary" :loading="submitting" @click="publish"><Send :size="17" aria-hidden="true" />发布消息</el-button></div>
      </el-form>

      <aside class="message-preview">
        <div class="section-heading"><h2>移动端预览</h2><Smartphone :size="18" aria-hidden="true" /></div>
        <div class="preview-phone">
          <header><span>悦享音乐</span><Bell :size="16" aria-hidden="true" /></header>
          <article><span class="preview-icon"><Megaphone :size="18" aria-hidden="true" /></span><div><strong>{{ form.subject || '消息主题' }}</strong><p>{{ form.body || '消息正文会显示在这里。' }}</p><small>刚刚</small></div></article>
        </div>
        <div class="preview-note"><ShieldCheck :size="19" aria-hidden="true" /><div><strong>发布确认</strong><p>提交前请再次核对主题和正文。</p></div></div>
      </aside>
    </div>
  </div>
</template>

<script>
import { Bell, Megaphone, Send, ShieldCheck, Smartphone, Users } from 'lucide-vue';
import { adminApi } from '../api/index.js';
import { adminProblemMessage, createSingleFlight, publishConfirmedMessage } from '../ui/admin-model.js';

export default {
  name: 'MessagesView',
  components: { Bell, Megaphone, Send, ShieldCheck, Smartphone, Users },
  data: () => ({
    submitting: false,
    publishOnce: createSingleFlight(),
    error: '',
    success: '',
    form: { subject: '', body: '' },
    rules: {
      subject: [{ required: true, message: '请输入主题', trigger: 'blur' }],
      body: [{ required: true, message: '请输入正文', trigger: 'blur' }]
    }
  }),
  methods: {
    async publish() {
      return this.publishOnce(async () => {
        this.submitting = true;
        this.error = '';
        this.success = '';
        try {
          const valid = await new Promise((resolve) => this.$refs.form.validate(resolve));
          if (!valid) return;
          const message = await publishConfirmedMessage({
            confirm: () => this.$confirm('发布后将立即进入所有用户的消息中心，确认继续？', '确认发布消息', {
              confirmButtonText: '确认发布', cancelButtonText: '取消', type: 'warning'
            }),
            publish: (payload) => adminApi.publishMessage(payload),
            payload: { ...this.form }
          });
          if (!message) return;
          this.success = `消息 #${message.id} 已发布。`;
          this.form = { subject: '', body: '' };
        } catch (problem) {
          this.error = adminProblemMessage(problem);
        } finally {
          this.submitting = false;
        }
      });
    }
  }
};
</script>
