<template>
  <div class="state-message" :class="`state-${state}`" role="status">
    <LoaderCircle v-if="state === 'loading'" class="spin" :size="20" aria-hidden="true" />
    <Info v-else-if="state === 'error'" :size="20" aria-hidden="true" />
    <span>{{ message }}</span>
  </div>
</template>

<script>
import { Info, LoaderCircle } from 'lucide-vue';
export default { name: 'StateMessage', components: { Info, LoaderCircle }, props: { state: { type: String, required: true }, message: { type: String, required: true } } };
</script>
