<script setup lang="ts">
import type { Category, Transaction } from '~/types'
import DashLayout from '~/layouts/DashLayout.vue'
import CustomHeader from '~/components/CustomHeader.vue'
import CategoryGoalReport from '~/components/goals/CategoryGoalReport.vue'

const { activeAccount, loadAccounts } = useFinancialAccounts()
const { $financesService } = useNuxtApp()
const toast = useToast()
const { t, locale } = useI18n()
const currentMonth = ref(new Date())
const monthlyIncome = ref(0)
const categories = ref<Category[]>([])
const monthTransactions = ref<Transaction[]>([])
const loading = ref(false)

const CLASSIFICATIONS = ['ESSENTIAL', 'INVESTMENT', 'RECREATION'] as const

const monthKey = computed(() => `${currentMonth.value.getFullYear()}-${String(currentMonth.value.getMonth() + 1).padStart(2, '0')}`)
const monthLabel = computed(() => new Intl.DateTimeFormat(locale.value === 'pt' ? 'pt-BR' : 'en-US', { month: 'long', year: 'numeric' }).format(currentMonth.value))

const categoryReport = computed(() => {
  const realizedByCategory = new Map<string, number>()
  for (const transaction of monthTransactions.value) {
    if (transaction.income || transaction.status !== 'OK' || !transaction.categoryID) continue
    if (monthOf(transaction.executionDate) !== monthKey.value) continue
    realizedByCategory.set(transaction.categoryID, (realizedByCategory.get(transaction.categoryID) || 0) + transaction.value)
  }
  const income = monthlyIncome.value
  return categories.value
    .filter(category => !category.income && realizedByCategory.has(category.id))
    .map((category) => {
      const realized = realizedByCategory.get(category.id) || 0
      const expected = category.goal > 0 ? income * category.goal / 100 : 0
      return {
        id: category.id,
        title: category.title,
        defaultKey: category.defaultKey,
        color: category.color,
        classification: category.classification || 'ESSENTIAL',
        goal: category.goal,
        expected,
        realized,
        progress: expected > 0 ? Math.min(realized / expected * 100, 100) : 0,
        achieved: expected > 0 ? realized / expected * 100 : null,
        share: income > 0 ? realized / income * 100 : null
      }
    })
    .sort((a, b) => b.realized - a.realized)
})

const classificationShares = computed(() => CLASSIFICATIONS.map((key) => {
  const realized = categoryReport.value
    .filter(row => (row.classification || 'ESSENTIAL').toUpperCase() === key)
    .reduce((total, row) => total + row.realized, 0)
  return {
    key,
    realized,
    share: monthlyIncome.value > 0 ? realized / monthlyIncome.value * 100 : null
  }
}))

function monthOf(value?: string | null) {
  return value ? /^(\d{4}-\d{2})/.exec(value)?.[1] || '' : ''
}

async function loadTransactions(accountId: string) {
  const size = 200
  const transactions: Transaction[] = []
  let page = 0
  let total = 0
  do {
    const response = await $financesService.getTransactions(accountId, page, size)
    transactions.push(...response.data.items)
    total = response.data.total
    page++
  } while (transactions.length < total && page < 50)
  return transactions
}

function incomeForMonth(transactions: Transaction[]) {
  return transactions
    .filter(transaction => transaction.income && transaction.status === 'OK' && monthOf(transaction.executionDate) === monthKey.value)
    .reduce((total, transaction) => total + transaction.value, 0)
}

let refreshSequence = 0
async function refreshReport() {
  const account = activeAccount.value
  if (!account) return
  const sequence = ++refreshSequence
  loading.value = true
  try {
    const [transactions, categoryResponse] = await Promise.all([
      loadTransactions(account.id),
      $financesService.getCategories()
    ])
    if (sequence !== refreshSequence || activeAccount.value?.id !== account.id) return
    categories.value = categoryResponse.data.items
    monthTransactions.value = transactions
    monthlyIncome.value = incomeForMonth(transactions)
  } finally {
    if (sequence === refreshSequence) loading.value = false
  }
}

watch(() => [activeAccount.value?.id, monthKey.value], () => {
  if (activeAccount.value) refreshReport().catch(() => {
    toast.add({ title: t('goalsLoadFailed'), color: 'error' })
  })
}, { immediate: true })

onMounted(() => loadAccounts().catch(() => {
  toast.add({ title: t('accountsLoadFailed'), color: 'error' })
}))

function changeMonth(delta: number) {
  currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() + delta, 1)
}
</script>

<template>
  <DashLayout>
    <UDashboardPanel id="goals">
      <template #header>
        <CustomHeader />
        <UDashboardToolbar>
          <div class="flex w-full flex-wrap items-center justify-between gap-3">
            <div class="text-sm text-muted">
              {{ activeAccount?.name || $t('noAccounts') }} · {{ $t('categoryGoalsTitle') }}
            </div>
            <div class="flex items-center gap-2">
              <UButton icon="i-lucide-chevron-left" color="neutral" variant="ghost" :aria-label="$t('previousMonth')" @click="changeMonth(-1)" />
              <span class="min-w-36 text-center font-medium capitalize">{{ monthLabel }}</span>
              <UButton icon="i-lucide-chevron-right" color="neutral" variant="ghost" :aria-label="$t('nextMonth')" @click="changeMonth(1)" />
            </div>
          </div>
        </UDashboardToolbar>
      </template>

      <template #body>
        <UAlert
          v-if="!activeAccount"
          color="neutral"
          variant="subtle"
          icon="i-lucide-wallet-cards"
          :title="$t('noAccounts')"
          :description="$t('createAccountFirst')"
        />

        <div v-else class="space-y-4">
          <UPageCard variant="subtle" :title="$t('classificationShareTitle')" :description="$t('classificationShareDesc')">
            <div v-if="loading" class="mt-4 grid gap-4 sm:grid-cols-3">
              <USkeleton v-for="index in 3" :key="index" class="h-24 rounded-lg" />
            </div>
            <div v-else-if="categoryReport.length" class="mt-4 grid gap-4 sm:grid-cols-3">
              <div
                v-for="item in classificationShares"
                :key="item.key"
                class="rounded-lg border border-default p-4"
              >
                <div class="flex items-center justify-between gap-2">
                  <span class="text-xs font-semibold tracking-wide text-muted uppercase">{{ $t(item.key.toLowerCase()) }}</span>
                  <span class="text-sm font-semibold tabular-nums">{{ item.share === null ? '—' : `${Math.round(item.share)}%` }}</span>
                </div>
                <p class="mt-2 text-xl font-semibold tabular-nums">
                  {{ formatCurrency(item.realized) }}
                </p>
                <p class="mt-1 text-xs text-muted">
                  {{ item.share === null ? $t('classificationShareNoIncome') : $t('categoryGoalShare', { value: Math.round(item.share) }) }}
                </p>
              </div>
            </div>
            <p v-else class="mt-4 py-6 text-center text-sm text-muted">
              {{ $t('categoryGoalsEmpty') }}
            </p>
          </UPageCard>

          <CategoryGoalReport :rows="categoryReport" :income="monthlyIncome" :loading="loading" />
        </div>
      </template>
    </UDashboardPanel>
  </DashLayout>
</template>
