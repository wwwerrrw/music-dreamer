<template>
  <AuthLayout title="账号已激活" :subtitle="subtitle">
    <router-link class="primary-button link-button" to="/login">前往登录</router-link>
  </AuthLayout>
</template>

<script>
import AuthLayout from '../components/AuthLayout.vue';

export default {
  name: 'ActivateView',
  components: { AuthLayout },
  computed: {
    subtitle() { return this.$route.query.email ? `${this.$route.query.email} 可以登录了。` : '账号可以登录了。'; }
  }
};
</script>
