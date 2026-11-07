<script setup lang="ts">
import type { Recurrence } from '~/types'

const props = defineProps({
  recurrence: {
    type: Object as PropType<Recurrence>,
    required: true
  }
})

const payedValue = computed(() =>
  props.recurrence.installments
    ? props.recurrence.value / props.recurrence.installments * (props.recurrence.payed ?? 0)
    : 0
)

const progressLabel = computed(() =>
  `${props.recurrence.payed ?? 0}/${props.recurrence.installments ?? 0}`
)

const emit = defineEmits<{
  (e: 'edit' | 'delete' | 'confirm', recurrence: Recurrence): void
}>()

const categoryLabel = useCategoryLabel()
</script>

<template>
  <div class="flex items-center justify-between gap-4 w-full">
    <div class="flex-1 flex flex-col gap-2">
      <div class="flex items-center gap-2">
        <UIcon
          name="i-lucide-repeat"
          class="text-muted"
        />
        <span class="font-medium">
          {{ props.recurrence.title }}
        </span>
      </div>

      <div class="text-sm text-muted">
        {{ categoryLabel(props.recurrence.category, props.recurrence.categoryKey) }}
      </div>

      <div class="relative w-full">
        <UProgress
          :model-value="payedValue"
          :max="props.recurrence.value"
          size="md"
          class="w-full"
        />

        <span
          class="absolute inset-0 flex items-center justify-center text-sm text-muted-foreground "
        >
          {{ progressLabel }}
        </span>
      </div>
    </div>

    <div class="flex items-center gap-3">
      <span class="font-semibold">
        {{ formatCurrency(props.recurrence.value) }}
      </span>

      <UDropdownMenu
        :items="[
          ...(props.recurrence.status === 'PENDING'
            ? [{ label: $t('confirm'), icon: 'i-lucide-check', onSelect: () => emit('confirm', recurrence) }]
            : []),
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
  </div>
</template>
