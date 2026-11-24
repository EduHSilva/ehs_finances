<script setup lang="ts">
import { designTokens } from '~/design-system/tokens'

defineProps<{
  compact?: boolean
}>()
</script>

<template>
  <span class="brand-logo inline-flex items-center gap-2 font-semibold">
    <span
      class="brand-logo-mark flex size-8 shrink-0 items-center justify-center rounded-[10px]"
      aria-hidden="true"
    >
      <UIcon :name="designTokens.brand.logoIcon" class="size-4" />
    </span>
    <span v-if="!compact" class="brand-logo-name font-display tracking-[-0.02em]">{{ designTokens.brand.name }}</span>
  </span>
</template>

<style scoped>
.brand-logo-name {
  font-size: 19px;
  font-weight: 600;
}

.brand-logo-mark {
  color: var(--brand-on-primary);
  background: linear-gradient(140deg, var(--brand-primary), var(--brand-primary-hover));
  box-shadow: inset 0 1px 0 color-mix(in srgb, var(--brand-on-primary) 24%, transparent), 0 8px 18px -10px color-mix(in srgb, var(--brand-primary-hover) 60%, transparent);
}
</style>
