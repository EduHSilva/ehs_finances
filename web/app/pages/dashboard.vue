<script setup lang="ts">
import type { CashflowProjection } from '~/types'
import DashLayout from '~/layouts/DashLayout.vue'
import CustomHeader from '~/components/CustomHeader.vue'

const { activeAccount, loadAccounts } = useFinancialAccounts()
const { $financesService } = useNuxtApp()
const { t } = useI18n()
const projection = ref<CashflowProjection | null>(null)
const loading = ref(false)
const error = ref(false)
const days = ref(90)
const dayOptions = [30, 90, 180].map(value => ({ label: t('daysValue', { count: value }), value }))

const series = computed(() => projection.value?.series || [])
const chartMin = computed(() => Math.min(0, ...series.value.map(point => point.balance)))
const chartMax = computed(() => Math.max(1, ...series.value.map(point => point.balance)))

async function refreshProjection() {
  if (!activeAccount.value) {
    projection.value = null
    return
  }
  loading.value = true
  error.value = false
  try {
    projection.value = (await $financesService.getProjection(activeAccount.value.id, days.value)).data
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

watch(() => [activeAccount.value?.id, days.value], refreshProjection, { immediate: true })

onMounted(async () => {
  try {
    await loadAccounts()
  } catch {
    error.value = true
  }
})
</script>

<template>
  <DashLayout>
    <UDashboardPanel id="home">
      <template #header>
        <CustomHeader />
        <UDashboardToolbar>
          <div class="flex w-full items-center justify-between gap-3">
            <span class="text-sm text-muted">{{ $t('projectionTitle') }}</span>
            <USelect v-model="days" :items="dayOptions" class="w-36" />
          </div>
        </UDashboardToolbar>
      </template>

      <template #body>
        <UAlert
          v-if="!activeAccount && !loading"
          color="neutral"
          variant="subtle"
          icon="i-lucide-wallet-cards"
          :title="$t('noAccounts')"
          :description="$t('createAccountFirst')"
        />
        <UAlert
          v-else-if="error"
          color="error"
          variant="subtle"
          icon="i-lucide-triangle-alert"
          :title="$t('projectionLoadFailed')"
          :actions="[{ label: $t('retry'), onClick: refreshProjection }]"
        />
        <div v-else-if="loading || !projection" class="grid gap-4 sm:grid-cols-2">
          <USkeleton class="h-28 rounded-lg" />
          <USkeleton class="h-28 rounded-lg" />
          <USkeleton class="h-72 rounded-lg sm:col-span-2" />
        </div>
        <div v-else class="space-y-4">
          <div class="grid gap-4 sm:grid-cols-3">
            <UPageCard variant="subtle" :title="$t('currentBalance')">
              <p class="text-2xl font-semibold">{{ formatCurrency(projection.currentBalance) }}</p>
            </UPageCard>
            <UPageCard variant="subtle" :title="$t('projectedClosingBalance')">
              <p class="text-2xl font-semibold">{{ formatCurrency(projection.closingBalance) }}</p>
            </UPageCard>
            <UPageCard variant="subtle" :title="$t('projectionConfidence')">
              <p class="text-2xl font-semibold capitalize">{{ projection.confidence }}</p>
            </UPageCard>
          </div>

          <div class="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
            <UPageCard variant="subtle" :title="$t('projectedCashflow')" :description="$t('projectionCalculatedAt', { date: formatDate(projection.calculatedAt) })">
              <div v-if="series.length" class="flex h-72 min-w-0 flex-col">
                <div class="flex min-h-0 flex-1 items-end gap-px overflow-hidden border-b border-default">
                  <div
                    v-for="(point, index) in series"
                    :key="point.date"
                    class="group relative flex h-full min-w-0 flex-1 items-end"
                    :title="`${formatDate(point.date)}: ${formatCurrency(point.balance)}`"
                  >
                    <div
                      class="w-full rounded-t bg-primary/70 transition-colors group-hover:bg-primary"
                      :style="{ height: `${Math.max(3, (point.balance - chartMin) / (chartMax - chartMin) * 100)}%` }"
                    />
                  </div>
                </div>
                <div class="relative h-6 shrink-0">
                  <span
                    v-for="(point, index) in series"
                    v-show="index === 0 || index === series.length - 1 || index % Math.max(1, Math.floor(series.length / 6)) === 0"
                    :key="`date-${point.date}`"
                    class="absolute top-1 whitespace-nowrap text-[10px] text-muted"
                    :class="index === 0 ? 'left-0' : index === series.length - 1 ? 'right-0' : '-translate-x-1/2'"
                    :style="index === 0 || index === series.length - 1 ? undefined : { left: `${index / (series.length - 1) * 100}%` }"
                  >
                    {{ formatDate(point.date) }}
                  </span>
                </div>
              </div>
              <p v-else class="py-12 text-center text-sm text-muted">{{ $t('noProjectionData') }}</p>
            </UPageCard>

            <UPageCard variant="subtle" :title="$t('projectionAssumptions')">
              <ul v-if="projection.assumptions.length" class="list-disc space-y-2 pl-5 text-sm text-muted">
                <li v-for="assumption in projection.assumptions" :key="assumption">{{ assumption }}</li>
              </ul>
              <p v-else class="text-sm text-muted">{{ $t('noProjectionAssumptions') }}</p>
            </UPageCard>
          </div>

          <UPageCard variant="subtle" :title="$t('projectionItems')">
            <div v-if="projection.items.length" class="divide-y divide-default">
              <div v-for="(item, index) in projection.items" :key="`${item.date}-${item.title}-${index}`" class="flex items-center justify-between gap-4 py-3">
                <div>
                  <p class="font-medium text-highlighted">{{ item.title }}</p>
                  <p class="text-sm text-muted">{{ formatDate(item.date) }} · {{ item.kind }}</p>
                </div>
                <span :class="item.income ? 'text-success' : 'text-error'">
                  {{ item.income ? '+' : '-' }}{{ formatCurrency(item.amount) }}
                </span>
              </div>
            </div>
            <p v-else class="py-6 text-center text-sm text-muted">{{ $t('noProjectionItems') }}</p>
          </UPageCard>
        </div>
      </template>
    </UDashboardPanel>
  </DashLayout>
</template>
