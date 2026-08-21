<template>
  <AuthLayout title="创建账号" subtitle="加入悦享音乐，开启你的专属音乐旅程。">
    <form class="auth-form" @submit.prevent="submit">
      <label class="auth-field"><span>用户名</span><span class="auth-input-shell"><UserRound :size="18" aria-hidden="true" /><input v-model.trim="form.username" autocomplete="username" maxlength="64" placeholder="请输入用户名" required></span></label>
      <label class="auth-field"><span>邮箱</span><span class="auth-input-shell"><Mail :size="18" aria-hidden="true" /><input v-model.trim="form.email" type="email" autocomplete="email" maxlength="254" placeholder="请输入邮箱" required></span></label>
      <label class="auth-field"><span>密码</span><span class="auth-input-shell"><LockKeyhole :size="18" aria-hidden="true" /><input v-model="form.password" type="password" autocomplete="new-password" placeholder="请输入密码" required></span></label>
      <label class="auth-field"><span>确认密码</span><span class="auth-input-shell"><ShieldCheck :size="18" aria-hidden="true" /><input v-model="form.confirmPassword" type="password" autocomplete="new-password" placeholder="请再次输入密码" required></span></label>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <button class="primary-button" type="submit" :disabled="submitting">{{ submitting ? '注册中…' : '创建账号' }}</button>
    </form>
    <p class="auth-switch">已有账号？<router-link to="/login">登录</router-link></p>
  </AuthLayout>
</template>

<script>
import { LockKeyhole, Mail, ShieldCheck, UserRound } from 'lucide-vue';
import AuthLayout from '../components/AuthLayout.vue';
import { api } from '../api.js';
import { registerAndActivate, registrationPasswordError } from '../auth/workflows.js';
import { problemMessage } from '../ui/problem-message.js';

export default {
  name: 'RegisterView',
  components: { AuthLayout, LockKeyhole, Mail, ShieldCheck, UserRound },
  data: () => ({ form: { username: '', email: '', password: '', confirmPassword: '' }, submitting: false, error: '' }),
  methods: {
    async submit() {
      const passwordError = registrationPasswordError(this.form.password, this.form.confirmPassword);
      if (passwordError) { this.error = passwordError; return; }
      this.submitting = true;
      this.error = '';
      try {
        const registration = { username: this.form.username, email: this.form.email, password: this.form.password };
        const result = await registerAndActivate({ api, registration });
        this.form.password = '';
        this.form.confirmPassword = '';
        await this.$router.replace({ path: '/activate', query: { email: result.email } });
      } catch (problem) {
        this.form.password = '';
        this.form.confirmPassword = '';
        this.error = problem.message === 'ACTIVATION_MESSAGE_NOT_FOUND'
          ? '激活消息尚未到达，请稍后重试。'
          : problemMessage(problem);
      } finally { this.submitting = false; }
    }
  }
};
</script>
