<template>
  <AuthLayout title="登录" subtitle="使用已激活的账号继续。">
    <form class="auth-form" @submit.prevent="submit">
      <label>账号或邮箱<input v-model.trim="form.login" autocomplete="username" required></label>
      <label>密码<input v-model="form.password" type="password" autocomplete="current-password" required></label>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <button class="primary-button" type="submit" :disabled="submitting">{{ submitting ? '登录中…' : '登录' }}</button>
    </form>
    <p class="auth-switch">还没有账号？<router-link to="/register">注册</router-link></p>
  </AuthLayout>
</template>

<script>
import AuthLayout from '../components/AuthLayout.vue';
import { problemMessage } from '../ui/problem-message.js';

export default {
  name: 'LoginView',
  components: { AuthLayout },
  data: () => ({ form: { login: '', password: '' }, submitting: false, error: '' }),
  methods: {
    async submit() {
      this.submitting = true;
      this.error = '';
      try {
        const result = await this.$store.dispatch('auth/login', { ...this.form });
        this.form.password = '';
        const redirect = typeof this.$route.query.redirect === 'string' ? this.$route.query.redirect : result.redirect;
        await this.$router.replace(redirect);
      } catch (problem) {
        this.form.password = '';
        this.error = problemMessage(problem);
      } finally { this.submitting = false; }
    }
  }
};
</script>
