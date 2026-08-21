<template>
  <main class="admin-login">
    <div class="login-brand"><span class="login-brand-mark"><Music2 :size="23" aria-hidden="true" /></span><strong>悦享音乐</strong><small>管理后台</small></div>
    <section>
      <header><span class="admin-login-icon"><ShieldCheck :size="24" aria-hidden="true" /></span><h1>管理员登录</h1><p>使用管理员账号进入工作台</p></header>
      <form @submit.prevent="submit">
        <label>账号或邮箱<span class="admin-input-shell"><UserRound :size="18" aria-hidden="true" /><input v-model.trim="form.login" autocomplete="username" placeholder="输入账号或邮箱" required></span></label>
        <label>密码<span class="admin-input-shell"><LockKeyhole :size="18" aria-hidden="true" /><input v-model="form.password" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" placeholder="输入密码" required><button class="admin-password-toggle" type="button" :aria-label="showPassword ? '隐藏密码' : '显示密码'" :title="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword"><EyeOff v-if="showPassword" :size="18" aria-hidden="true" /><Eye v-else :size="18" aria-hidden="true" /></button></span></label>
        <p v-if="error" class="admin-error" role="alert">{{ error }}</p>
        <button class="admin-login-submit" type="submit" :disabled="submitting">{{ submitting ? '登录中…' : '登录' }}</button>
      </form>
    </section>
  </main>
</template>

<script>
import { Eye, EyeOff, LockKeyhole, Music2, ShieldCheck, UserRound } from 'lucide-vue';
import { adminProblemMessage } from '../ui/admin-model.js';

export default {
  name: 'LoginView',
  components: { Eye, EyeOff, LockKeyhole, Music2, ShieldCheck, UserRound },
  data: () => ({ form: { login: '', password: '' }, showPassword: false, submitting: false, error: '' }),
  methods: {
    async submit() {
      this.submitting = true;
      this.error = '';
      try {
        await this.$store.dispatch('login', { ...this.form });
        this.form.password = '';
        this.$router.replace(typeof this.$route.query.redirect === 'string' ? this.$route.query.redirect : '/tracks');
      } catch (problem) {
        this.form.password = '';
        this.error = adminProblemMessage(problem);
      } finally {
        this.submitting = false;
      }
    }
  }
};
</script>
