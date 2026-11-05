<script setup lang="ts">
import type { Recurrence } from '~/types'

const props = defineProps({
  recurrence: {
    type: Object as PropType<Recurrence>,
    required: true
  }
})

const emit = defineEmits<{
  (e: 'edit' | 'delete', recurrence: Recurrence): void
}>()
</script>

<template>
  <div class="flex flex-col gap-1">
    <div class="flex items-center gap-2">
      <UIcon
        name="i-lucide-repeat"
        class="text-muted"
      />

      <span class="font-medium">
        {{ props.recurrence.title }}
      </span>

      <UBadge
        size="xs"
        :color="props.recurrence.status === 'active' ? 'success' : 'warning'"
        variant="subtle"
      >
        {{ props.recurrence.status }}
      </UBadge>
    </div>

    <div class="text-sm text-muted flex flex-wrap gap-1">
      <span>{{ props.recurrence.income ? $t('income') : $t('expanse') }}</span> •
      <span>{{ props.recurrence.day }}</span>
      <template v-if="props.recurrence.category">
        • <span>{{ props.recurrence.category }}</span>
      </template>
    </div>
  </div>

  <div class="flex items-center gap-4">
    <span
      class="font-semibold"
      :class="props.recurrence.income ? 'text-success' : 'text-error'"
    >
      {{ formatCurrency(props.recurrence.value) }}
    </span>

    <UDropdownMenu
      :items="[
        { label: $t('edit'), icon: 'i-lucide-edit', onSelect: () => emit('edit', recurrence) },
        { label: $t('delete'), icon: 'i-lucide-trash', color: 'error', onSelect: () => emit('delete', recurrence) }
      ]"
      :content="{ align: 'end' }"
    >
      <UButton
        icon="i-lucide-more-vertical"
        variant="ghost"
        color="neutral"
        size="sm"
      />
    </UDropdownMenu>
  </div>
</template>

<style scoped>

</style>
