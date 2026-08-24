<template>
  <AuthLayout immersive title="欢迎回来" subtitle="准备好开启悦耳之旅了吗？">
    <form class="auth-form" @submit.prevent="submit">
      <label class="auth-field">
        <span>账号或邮箱</span>
        <span class="auth-input-shell"><UserRound :size="18" aria-hidden="true" /><input v-model.trim="form.login" autocomplete="username" placeholder="请输入账号或邮箱" required></span>
      </label>
      <label class="auth-field">
        <span>密码</span>
        <span class="auth-input-shell"><LockKeyhole :size="18" aria-hidden="true" /><input v-model="form.password" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" placeholder="请输入密码" required><button class="password-toggle" type="button" :aria-label="showPassword ? '隐藏密码' : '显示密码'" :title="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword"><EyeOff v-if="showPassword" :size="18" /><Eye v-else :size="18" /></button></span>
      </label>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <button class="primary-button" type="submit" :disabled="submitting">{{ submitting ? '登录中…' : '登录' }}</button>
    </form>
    <p class="auth-switch">还没有账号？<router-link to="/register">注册</router-link></p>
  </AuthLayout>
</template>

<script>
import { Eye, EyeOff, LockKeyhole, UserRound } from 'lucide-vue';
import AuthLayout from '../components/AuthLayout.vue';
import { problemMessage } from '../ui/problem-message.js';

export default {
  name: 'LoginView',
  components: { AuthLayout, Eye, EyeOff, LockKeyhole, UserRound },
  data: () => ({ form: { login: '', password: '' }, submitting: false, error: '', showPassword: false }),
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
