<script setup lang="ts">
import type { CashflowProjection, DashboardPending, InvestmentSummary, PendingItem, Recurrence } from '~/types'
import DashLayout from '~/layouts/DashLayout.vue'
import CustomHeader from '~/components/CustomHeader.vue'

const { activeAccount, loadAccounts } = useFinancialAccounts()
const { $financesService } = useNuxtApp()
const { t, te } = useI18n()
const toast = useToast()
const projection = ref<CashflowProjection | null>(null)
const recurrences = ref<Recurrence[]>([])
const pending = ref<DashboardPending | null>(null)
const investmentSummary = ref<InvestmentSummary | null>(null)
const loading = ref(false)
const error = ref(false)
const days = ref(30)
const dayOptions = [30, 90, 180].map(value => ({ label: t('daysValue', { count: value }), value }))

function projectionKindLabel(kind: string) {
  const key = `projectionKind.${kind.toLowerCase()}`
  return te(key) ? t(key) : kind
}

const installmentLabels = computed(() => {
  const result: Record<number, string> = {}
  if (!projection.value) return result

  const items = projection.value.items
    .map((item, index) => ({ item, index }))
    .filter(({ item }) => item.kind.toLowerCase() === 'installment')
    .sort((a, b) => a.item.date.localeCompare(b.item.date))
  const positions = new Map<string, number>()

  for (const { item, index } of items) {
    const recurrence = recurrences.value.find((candidate) =>
      item.recurrenceId
        ? candidate.id === item.recurrenceId
        : candidate.type.toUpperCase() === 'INSTALLMENT' && candidate.title === item.title
    )
    const total = item.installments ?? recurrence?.installments
    if (!total) continue

    const group = item.recurrenceId || recurrence?.id || item.title
    const position = (positions.get(group) ?? item.payed ?? recurrence?.payed ?? 0) + 1
    positions.set(group, position)
    const current = item.installmentNumber ?? position
    result[index] = t('installmentProgress', { current, total })
  }

  return result
})

const series = computed(() => projection.value?.series || [])
const chartMin = computed(() => Math.min(0, ...series.value.map(point => point.balance)))
const chartMax = computed(() => Math.max(1, ...series.value.map(point => point.balance)))
const yAxisLabels = computed(() => [chartMax.value, (chartMax.value + chartMin.value) / 2, chartMin.value])

async function refreshProjection() {
  if (!activeAccount.value) {
    projection.value = null
    recurrences.value = []
    pending.value = null
    investmentSummary.value = null
    return
  }
  loading.value = true
  error.value = false
  try {
    const accountId = activeAccount.value.id
    const [projectionResponse, recurrenceResponse, pendingResponse, investmentResponse] = await Promise.all([
      $financesService.getProjection(accountId, days.value),
      $financesService.getRecurrences(accountId).catch(() => null),
      $financesService.getPending(accountId, days.value).catch(() => null),
      $financesService.getInvestmentSummary(accountId).catch(() => null)
    ])
    projection.value = projectionResponse.data
    recurrences.value = recurrenceResponse?.data.items || []
    pending.value = pendingResponse?.data || null
    investmentSummary.value = investmentResponse?.data || null
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

async function confirmPending(item: PendingItem) {
  try {
    if (item.kind === 'TRANSACTION') await $financesService.confirmTransaction(item.id)
    else await $financesService.confirmRecurrence(item.id)
    toast.add({ title: t('pendingConfirmed'), color: 'success', icon: 'i-lucide-check' })
    await refreshProjection()
  } catch {
    toast.add({ title: t('pendingConfirmFailed'), color: 'error' })
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
          <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <UPageCard variant="subtle" :title="$t('currentBalance')">
              <p class="text-2xl font-semibold">{{ formatCurrency(projection.currentBalance) }}</p>
            </UPageCard>
            <UPageCard variant="subtle" :title="$t('projectedClosingBalance')">
              <p class="text-2xl font-semibold">{{ formatCurrency(projection.closingBalance) }}</p>
            </UPageCard>
            <UPageCard variant="subtle" :title="$t('investmentBalance')">
              <p class="text-2xl font-semibold">{{ formatCurrency(investmentSummary?.balance || 0) }}</p>
              <p v-if="investmentSummary" class="mt-1 text-xs text-muted">
                {{ $t('contributions') }} {{ formatCurrency(investmentSummary.totalContributions) }} ·
                {{ $t('withdrawals') }} {{ formatCurrency(investmentSummary.totalWithdrawals) }}
              </p>
              <UButton
                to="/goals"
                :label="$t('manageGoals')"
                size="xs"
                variant="link"
                class="mt-2 p-0"
              />
            </UPageCard>
            <UPageCard variant="subtle" :title="$t('projectionConfidence')">
              <p class="text-2xl font-semibold">{{ $t(`confidence.${projection.confidence.toLowerCase()}`) }}</p>
            </UPageCard>
          </div>

          <div class="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
            <UPageCard variant="subtle" :title="$t('projectedCashflow')" :description="$t('projectionCalculatedAt', { date: formatDate(projection.calculatedAt) })">
              <div v-if="series.length" class="flex h-72 min-w-0 flex-col">
                <div class="flex min-h-0 flex-1 gap-2">
                  <div class="flex h-full w-20 shrink-0 flex-col justify-between pb-px text-right text-[10px] text-muted">
                    <span v-for="(value, index) in yAxisLabels" :key="index">{{ formatCurrency(value) }}</span>
                  </div>
                  <div class="flex min-w-0 flex-1 items-end gap-px overflow-hidden border-b border-default">
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

          <UPageCard variant="subtle" :title="$t('pendingItems')" :description="$t('pendingItemsHelp')">
            <div v-if="pending?.items.length" class="divide-y divide-default">
              <div v-for="item in pending.items" :key="`${item.kind}-${item.id}`" class="flex flex-wrap items-center justify-between gap-3 py-3">
                <div>
                  <p class="font-medium text-highlighted">{{ item.title }}</p>
                  <p class="text-sm text-muted">
                    {{ formatDate(item.date) }} · {{ $t(`pendingKind.${item.kind.toLowerCase()}`) }} · {{ $t(`transactionStatus.${item.status.toLowerCase()}`) }}
                  </p>
                </div>
                <div class="flex items-center gap-3">
                  <span :class="item.income ? 'text-success' : 'text-error'">
                    {{ item.income ? '+' : '-' }}{{ formatCurrency(item.amount) }}
                  </span>
                  <UButton size="xs" icon="i-lucide-check" :label="$t('confirm')" @click="confirmPending(item)" />
                </div>
              </div>
            </div>
            <p v-else class="py-6 text-center text-sm text-muted">{{ $t('noPendingItems') }}</p>
          </UPageCard>

          <UPageCard variant="subtle" :title="$t('projectionItems')">
            <div v-if="projection.items.length" class="divide-y divide-default">
              <div v-for="(item, index) in projection.items" :key="`${item.date}-${item.title}-${index}`" class="flex items-center justify-between gap-4 py-3">
                <div>
                  <p class="font-medium text-highlighted">{{ item.title }}</p>
                  <p class="text-sm text-muted">
                    {{ formatDate(item.date) }} · {{ projectionKindLabel(item.kind) }}
                    <span v-if="installmentLabels[index]"> · {{ installmentLabels[index] }}</span>
                  </p>
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
