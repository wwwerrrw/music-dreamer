<template>
  <router-view v-if="isPublicRoute" />
  <AppShell v-else />
</template>

<script>
import AppShell from './components/AppShell.vue';

export default {
  name: 'App',
  components: { AppShell },
  computed: {
    isPublicRoute() { return this.$route.meta.requiresAuth === false; }
  }
};
</script>
