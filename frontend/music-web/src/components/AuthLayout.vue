<template>
  <main class="auth-layout" :class="{ 'is-immersive': immersive }">
    <section v-if="immersive" class="auth-visual" aria-hidden="true">
      <img src="/assets/studio-piano.jpg" alt="">
    </section>
    <div class="auth-content">
      <section class="auth-panel" :aria-labelledby="titleId">
        <header class="auth-header">
          <router-link class="auth-brand" to="/login" aria-label="悦享音乐首页">
            <span class="auth-brand-mark"><Music2 :size="24" :stroke-width="2.4" aria-hidden="true" /></span>
            <span>悦享音乐</span>
          </router-link>
          <h1 :id="titleId">{{ title }}</h1>
          <p v-if="subtitle" class="auth-subtitle">{{ subtitle }}</p>
        </header>
        <slot />
      </section>
      <footer class="auth-footer">© 2026 悦享音乐</footer>
    </div>
  </main>
</template>

<script>
import { Music2 } from 'lucide-vue';

export default {
  name: 'AuthLayout',
  components: { Music2 },
  props: {
    title: { type: String, required: true },
    subtitle: { type: String, default: '' },
    immersive: { type: Boolean, default: false }
  },
  computed: { titleId() { return `auth-title-${this._uid}`; } }
};
</script>
