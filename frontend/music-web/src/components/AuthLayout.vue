<template>
  <main class="auth-layout">
    <router-link class="auth-brand" to="/login">MUSIC <span>DREAMER</span></router-link>
    <section class="auth-panel" :aria-labelledby="titleId">
      <h1 :id="titleId">{{ title }}</h1>
      <p v-if="subtitle" class="auth-subtitle">{{ subtitle }}</p>
      <slot />
    </section>
  </main>
</template>

<script>
export default {
  name: 'AuthLayout',
  props: { title: { type: String, required: true }, subtitle: { type: String, default: '' } },
  computed: { titleId() { return `auth-title-${this._uid}`; } }
};
</script>
