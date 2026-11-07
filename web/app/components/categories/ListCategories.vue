<script setup lang="ts">
import type { Category } from '~/types'

const props = defineProps({
  data: {
    type: Array as PropType<Category[]>,
    required: true
  },
  title: String
})

const emit = defineEmits<{
  (e: 'edit' | 'delete', category: Category): void
}>()

const categoryLabel = useCategoryLabel()
</script>

<template>
  <div class="w-full h-full ml-2">
    {{ props.title }}
    <div
      v-for="(category, index) in props.data"
      :key="index"
    >
      <UPageCard
        variant="subtle"
        class="mb-2 h-20"
      >
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="text-sm font-medium">
              {{ categoryLabel(category.title, category.defaultKey) }}
            </p>

            <div class="flex items-center gap-1.5 text-xs text-muted">
              <span
                class="inline-block w-2.5 h-2.5 rounded-full"
                :style="{ backgroundColor: category.color }"
              />
              <UBadge
                size="xs"
                :color="category.income ? 'success' : 'neutral'"
                variant="subtle"
              >
                {{ category.income ? $t('income') : $t('expanse') }}
              </UBadge>
              <span v-if="!category.income">{{ $t(category.classification?.toLowerCase() || 'essential') }}</span>
            </div>
          </div>

          <div class="flex items-center gap-3">
            <div v-if="!category.income" class="text-right">
              <p class="text-xs text-muted">
                {{ $t('goal') }}
              </p>
              <p class="text-base font-semibold leading-tight">
                {{ category.goal }}%
              </p>
            </div>

            <UDropdownMenu
              :items="[
                { label: $t('edit'), icon: 'i-lucide-pencil', onSelect: () => emit('edit', category) },
                { label: $t('delete'), icon: 'i-lucide-trash', color: 'error', onSelect: () => emit('delete', category) }
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
      </UPageCard>
    </div>
  </div>
</template>

<style scoped>

</style>
