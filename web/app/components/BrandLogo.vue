<script setup lang="ts">
import { designTokens } from '~/design-system/tokens'

defineProps<{
  compact?: boolean
}>()
</script>

<template>
  <span class="inline-flex items-center gap-2 font-semibold">
    <span
      class="flex size-8 shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-indigo-600 to-violet-600 text-white shadow-sm"
      aria-hidden="true"
    >
      <UIcon :name="designTokens.brand.logoIcon" class="size-4" />
    </span>
    <span v-if="!compact" class="tracking-tight">{{ designTokens.brand.name }}</span>
  </span>
</template>
