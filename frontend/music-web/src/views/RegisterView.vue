<template>
  <AuthLayout title="注册" subtitle="注册后将自动完成本地开发激活。">
    <form class="auth-form" @submit.prevent="submit">
      <label>用户名<input v-model.trim="form.username" autocomplete="username" maxlength="64" required></label>
      <label>邮箱<input v-model.trim="form.email" type="email" autocomplete="email" maxlength="254" required></label>
      <label>密码<input v-model="form.password" type="password" autocomplete="new-password" required></label>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <button class="primary-button" type="submit" :disabled="submitting">{{ submitting ? '注册中…' : '创建账号' }}</button>
    </form>
    <p class="auth-switch">已有账号？<router-link to="/login">登录</router-link></p>
  </AuthLayout>
</template>

<script>
import AuthLayout from '../components/AuthLayout.vue';
import { api } from '../api.js';
import { registerAndActivate } from '../auth/workflows.js';
import { problemMessage } from '../ui/problem-message.js';

export default {
  name: 'RegisterView',
  components: { AuthLayout },
  data: () => ({ form: { username: '', email: '', password: '' }, submitting: false, error: '' }),
  methods: {
    async submit() {
      this.submitting = true;
      this.error = '';
      try {
        const result = await registerAndActivate({ api, registration: { ...this.form } });
        this.form.password = '';
        await this.$router.replace({ path: '/activate', query: { email: result.email } });
      } catch (problem) {
        this.form.password = '';
        this.error = problem.message === 'ACTIVATION_MESSAGE_NOT_FOUND'
          ? '激活消息尚未到达，请稍后重试。'
          : problemMessage(problem);
      } finally { this.submitting = false; }
    }
  }
};
</script>
