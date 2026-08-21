<template>
  <AuthLayout title="账号启用成功" :subtitle="subtitle">
    <div class="activation-result" aria-hidden="true"><BadgeCheck :size="38" /></div>
    <router-link class="primary-button link-button" to="/login">前往登录</router-link>
    <p class="activation-help">如果链接已失效，请返回注册页面重新开始。</p>
  </AuthLayout>
</template>

<script>
import { BadgeCheck } from 'lucide-vue';
import AuthLayout from '../components/AuthLayout.vue';

export default {
  name: 'ActivateView',
  components: { AuthLayout, BadgeCheck },
  computed: {
    subtitle() { return this.$route.query.email ? `${this.$route.query.email} 可以登录了。` : '账号可以登录了。'; }
  }
};
</script>
