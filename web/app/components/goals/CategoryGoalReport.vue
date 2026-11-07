<script setup lang="ts">
interface CategoryGoalRow {
  id: string
  title: string
  defaultKey?: string | null
  color: string
  goal: number
  expected: number
  realized: number
  progress: number
  achieved: number | null
  share: number | null
}

const props = defineProps<{
  rows: CategoryGoalRow[]
  income: number
  loading?: boolean
}>()

const categoryLabel = useCategoryLabel()

const totalExpected = computed(() => props.rows.reduce((total, row) => total + row.expected, 0))
const totalRealized = computed(() => props.rows.reduce((total, row) => total + row.realized, 0))
</script>

<template>
  <UPageCard variant="subtle" :title="$t('categoryGoalsTitle')" :description="$t('categoryGoalsDesc')">
    <div v-if="loading" class="mt-4 space-y-4">
      <USkeleton v-for="index in 3" :key="index" class="h-10 rounded-md" />
    </div>

    <div v-else-if="rows.length" class="mt-4 space-y-5">
      <div class="flex flex-wrap items-center justify-between gap-2 text-sm text-muted">
        <span>{{ $t('categoryGoalsIncome', { income: formatCurrency(income) }) }}</span>
        <span>
          {{ $t('expected') }} {{ formatCurrency(totalExpected) }} ·
          {{ $t('realized') }} {{ formatCurrency(totalRealized) }}
        </span>
      </div>

      <div v-for="row in rows" :key="row.id" class="space-y-1.5">
        <div class="flex items-center justify-between gap-3">
          <div class="flex min-w-0 items-center gap-2">
            <span class="size-2.5 shrink-0 rounded-full" :style="{ backgroundColor: row.color }" />
            <span class="truncate text-sm font-medium">{{ categoryLabel(row.title, row.defaultKey) }}</span>
            <UBadge size="xs" color="neutral" variant="subtle">
              {{ row.goal }}%
            </UBadge>
          </div>
          <span class="shrink-0 text-sm">
            <span class="font-medium">{{ formatCurrency(row.realized) }}</span>
            <span class="text-muted"> / {{ row.goal > 0 ? formatCurrency(row.expected) : $t('noGoalSet') }}</span>
          </span>
        </div>

        <div class="h-2 overflow-hidden rounded-full bg-elevated">
          <div
            class="h-full rounded-full transition-[width] duration-300"
            :style="{ width: `${row.progress}%`, backgroundColor: row.color }"
          />
        </div>

        <div class="flex items-center justify-between gap-2 text-xs text-muted">
          <span>
            {{ row.achieved !== null
              ? $t('categoryGoalAchieved', { value: Math.round(row.achieved) })
              : row.goal > 0 ? $t('categoryGoalNoIncome') : $t('noGoalSet') }}
          </span>
          <span v-if="row.share !== null">{{ $t('categoryGoalShare', { value: Math.round(row.share) }) }}</span>
        </div>
      </div>
    </div>

    <p v-else class="mt-4 py-6 text-center text-sm text-muted">
      {{ $t('categoryGoalsEmpty') }}
    </p>
  </UPageCard>
</template>
