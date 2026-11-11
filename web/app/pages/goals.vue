<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'
import type { Category, MonthlyGoal, MonthlyGoalMode, Transaction } from '~/types'
import type { MonthlyGoalRequest } from '~/services/financesService'
import DashLayout from '~/layouts/DashLayout.vue'
import CustomHeader from '~/components/CustomHeader.vue'
import CategoryGoalReport from '~/components/goals/CategoryGoalReport.vue'

const { activeAccount, loadAccounts } = useFinancialAccounts()
const { $financesService } = useNuxtApp()
const { goals, summaries, loading, load, create, update, remove, contribute } = useMonthlyGoals()
const toast = useToast()
const { t, locale } = useI18n()
const currentMonth = ref(new Date())
const monthlyIncome = ref(0)
const categories = ref<Category[]>([])
const monthTransactions = ref<Transaction[]>([])
const newGoalOpen = ref(false)
const contributionOpen = ref(false)
const selectedGoal = ref<MonthlyGoal | null>(null)
const editingGoal = ref<MonthlyGoal | null>(null)

const createSchema = z.object({
  title: z.string().trim().min(2),
  mode: z.enum(['FIXED', 'PERCENT_INCOME']),
  amount: z.coerce.number().positive(),
  initialBalance: z.coerce.number().min(0).optional(),
  objective: z.coerce.number().min(0).optional(),
  dtInicio: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),
  dtFim: z.union([z.literal(''), z.string().regex(/^\d{4}-\d{2}-\d{2}$/)])
}).refine(value => value.mode !== 'PERCENT_INCOME' || value.amount <= 100, {
  path: ['amount'],
  message: t('percentageMaximum')
}).refine(value => !value.dtFim || value.dtFim >= value.dtInicio, {
  path: ['dtFim'],
  message: t('goalEndBeforeStart')
})
const contributionSchema = z.object({
  amount: z.coerce.number().positive(),
  date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/)
})

type CreateSchema = z.output<typeof createSchema>
type ContributionSchema = z.output<typeof contributionSchema>

const createState = reactive<Partial<CreateSchema>>({
  title: '', mode: 'FIXED', amount: undefined, initialBalance: undefined, objective: undefined, dtInicio: '', dtFim: ''
})
const contributionState = reactive<Partial<ContributionSchema>>({ amount: undefined, date: '' })

const monthKey = computed(() => `${currentMonth.value.getFullYear()}-${String(currentMonth.value.getMonth() + 1).padStart(2, '0')}`)
const monthLabel = computed(() => new Intl.DateTimeFormat(locale.value === 'pt' ? 'pt-BR' : 'en-US', { month: 'long', year: 'numeric' }).format(currentMonth.value))
const accountGoals = computed(() => goals.value)

function expectedFor(goal: MonthlyGoal) {
  return summaries.value[goal.id]?.expected || 0
}

function realizedFor(goal: MonthlyGoal) {
  return summaries.value[goal.id]?.realized || 0
}

function initialBalanceFor(goal: MonthlyGoal) {
  return summaries.value[goal.id]?.initialBalance ?? goal.initialBalance ?? 0
}

function savedFor(goal: MonthlyGoal) {
  return summaries.value[goal.id]?.saved ?? goal.initialBalance ?? 0
}

function objectiveFor(goal: MonthlyGoal) {
  return summaries.value[goal.id]?.objective ?? goal.objective ?? null
}

function objectiveProgressFor(goal: MonthlyGoal) {
  return summaries.value[goal.id]?.objectiveProgress ?? 0
}

const initialBalanceEditable = computed(() => {
  const goal = editingGoal.value
  if (!goal) return true
  return savedFor(goal) <= 0
})

const expectedTotal = computed(() => accountGoals.value.reduce((total, goal) => total + expectedFor(goal), 0))
const realizedTotal = computed(() => accountGoals.value.reduce((total, goal) => total + realizedFor(goal), 0))

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
async function refreshGoals() {
  const account = activeAccount.value
  if (!account) return
  const sequence = ++refreshSequence
  const [, transactions, categoryResponse] = await Promise.all([
    load(account.id, monthKey.value),
    loadTransactions(account.id),
    $financesService.getCategories()
  ])
  if (sequence !== refreshSequence || activeAccount.value?.id !== account.id) return
  categories.value = categoryResponse.data.items
  monthTransactions.value = transactions
  monthlyIncome.value = goals.value.length
    ? summaries.value[goals.value[0]!.id]?.incomeBase || 0
    : incomeForMonth(transactions)
}

watch(() => [activeAccount.value?.id, monthKey.value], () => {
  if (activeAccount.value) refreshGoals().catch(() => {
    toast.add({ title: t('goalsLoadFailed'), color: 'error' })
  })
}, { immediate: true })

onMounted(() => loadAccounts().catch(() => {
  toast.add({ title: t('accountsLoadFailed'), color: 'error' })
}))

function changeMonth(delta: number) {
  currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() + delta, 1)
}

function openContribution(goal: MonthlyGoal) {
  selectedGoal.value = goal
  contributionState.amount = undefined
  contributionState.date = `${monthKey.value}-01`
  contributionOpen.value = true
}

function monthBoundary(day: 'start' | 'end') {
  if (day === 'start') return `${monthKey.value}-01`
  const [year, month] = monthKey.value.split('-').map(Number)
  return `${monthKey.value}-${String(new Date(year!, month!, 0).getDate()).padStart(2, '0')}`
}

function openGoalEditor(goal?: MonthlyGoal) {
  editingGoal.value = goal || null
  Object.assign(createState, goal
    ? {
        title: goal.title,
        mode: goal.mode,
        amount: goal.amount,
        initialBalance: goal.initialBalance ?? 0,
        objective: goal.objective ?? undefined,
        dtInicio: goal.dtInicio || `${goal.periodMonth}-01`,
        dtFim: goal.dtFim || ''
      }
    : {
        title: '',
        mode: 'FIXED',
        amount: undefined,
        initialBalance: undefined,
        objective: undefined,
        dtInicio: monthBoundary('start'),
        dtFim: monthBoundary('end')
      })
  newGoalOpen.value = true
}

function goalPeriodLabel(goal: MonthlyGoal) {
  const start = goal.dtInicio || `${goal.periodMonth}-01`
  if (!goal.dtFim) return `${formatDate(start)} · ${t('noEndDate')}`
  return `${formatDate(start)} – ${formatDate(goal.dtFim)}`
}

async function onCreateGoal(event: FormSubmitEvent<CreateSchema>) {
  const account = activeAccount.value
  if (!account) return
  const wasEditing = Boolean(editingGoal.value)
  const request: MonthlyGoalRequest = {
    accountId: account.id,
    title: event.data.title,
    mode: event.data.mode,
    amount: event.data.amount,
    initialBalance: event.data.initialBalance ?? 0,
    objective: event.data.objective && event.data.objective > 0 ? event.data.objective : null,
    periodMonth: event.data.dtInicio.slice(0, 7),
    dtInicio: event.data.dtInicio,
    dtFim: event.data.dtFim || null,
    active: true
  }
  try {
    if (editingGoal.value) await update(editingGoal.value.id, request, account.id, monthKey.value)
    else await create(request, account.id, monthKey.value)
    newGoalOpen.value = false
    editingGoal.value = null
    Object.assign(createState, {
      title: '', mode: 'FIXED', amount: undefined, initialBalance: undefined, objective: undefined, dtInicio: '', dtFim: ''
    })
    toast.add({ title: t(wasEditing ? 'goalUpdated' : 'goalCreated'), color: 'success', icon: 'i-lucide-check' })
  } catch {
    toast.add({ title: t('goalSaveFailed'), color: 'error' })
  }
}

async function onAddContribution(event: FormSubmitEvent<ContributionSchema>) {
  const account = activeAccount.value
  if (!selectedGoal.value || !account) return
  try {
    await contribute(selectedGoal.value.id, event.data, account.id, monthKey.value)
    contributionOpen.value = false
    toast.add({ title: t('goalContributionAdded'), color: 'success', icon: 'i-lucide-check' })
  } catch {
    toast.add({ title: t('goalContributionFailed'), color: 'error' })
  }
}

async function onDeleteGoal(goal: MonthlyGoal) {
  const account = activeAccount.value
  if (!account) return
  try {
    await remove(goal.id, account.id, monthKey.value)
    toast.add({ title: t('goalDeleted'), color: 'neutral' })
  } catch {
    toast.add({ title: t('goalDeleteFailed'), color: 'error' })
  }
}

const modeItems = computed(() => [
  { label: t('fixedMonthlyAmount'), value: 'FIXED' as MonthlyGoalMode },
  { label: t('percentOfIncome'), value: 'PERCENT_INCOME' as MonthlyGoalMode }
])
</script>

<template>
  <DashLayout>
    <UDashboardPanel id="goals">
      <template #header>
        <CustomHeader>
          <UButton
            v-if="activeAccount"
            :label="$t('newGoal')"
            icon="i-lucide-plus"
            class="bg-primary dark:bg-primary-dark"
            @click="openGoalEditor()"
          />
        </CustomHeader>
        <UDashboardToolbar>
          <div class="flex w-full flex-wrap items-center justify-between gap-3">
            <div class="text-sm text-muted">
              {{ activeAccount?.name || $t('noAccounts') }} · {{ $t('monthlyGoals') }}
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
          <div class="grid gap-4 sm:grid-cols-3">
            <UPageCard variant="subtle" :title="$t('expectedThisMonth')">
              <p class="text-2xl font-semibold">
                {{ formatCurrency(expectedTotal) }}
              </p>
            </UPageCard>
            <UPageCard variant="subtle" :title="$t('realizedThisMonth')">
              <p class="text-2xl font-semibold text-primary">
                {{ formatCurrency(realizedTotal) }}
              </p>
            </UPageCard>
            <UPageCard variant="subtle" :title="$t('monthlyIncomeBase')">
              <p class="text-2xl font-semibold">
                {{ formatCurrency(monthlyIncome) }}
              </p>
            </UPageCard>
          </div>

          <UAlert color="neutral" variant="subtle" icon="i-lucide-info" :title="$t('goalRealizedHelp')" />

          <div v-if="loading" class="grid gap-4 xl:grid-cols-2">
            <USkeleton v-for="index in 2" :key="index" class="h-52 rounded-lg" />
          </div>

          <div v-else-if="accountGoals.length" class="grid gap-4 xl:grid-cols-2">
            <UPageCard v-for="goal in accountGoals" :key="goal.id" variant="subtle">
              <div class="space-y-4">
                <div class="flex items-start justify-between gap-3">
                  <div>
                    <h2 class="font-semibold text-highlighted">
                      {{ goal.title }}
                    </h2>
                    <p class="text-sm text-muted">
                      {{ goal.mode === 'FIXED' ? $t('fixedMonthlyAmount') : `${goal.amount}% ${$t('ofIncome')}` }}
                    </p>
                    <p class="text-xs text-muted">
                      {{ goalPeriodLabel(goal) }}
                      <UBadge v-if="goal.recurring" size="xs" color="primary" variant="subtle" class="ml-1">
                        {{ $t('recurringGoal') }}
                      </UBadge>
                    </p>
                    <p v-if="initialBalanceFor(goal) > 0" class="text-xs text-muted">
                      {{ $t('initialBalance') }}: {{ formatCurrency(initialBalanceFor(goal)) }}
                    </p>
                  </div>
                  <UDropdownMenu
                    :items="[
                      { label: $t('edit'), icon: 'i-lucide-pencil', onSelect: () => openGoalEditor(goal) },
                      { label: $t('delete'), icon: 'i-lucide-trash', color: 'error', onSelect: () => onDeleteGoal(goal) }
                    ]"
                    :content="{ align: 'end' }"
                  >
                    <UButton icon="i-lucide-ellipsis-vertical" color="neutral" variant="ghost" :aria-label="$t('actions')" />
                  </UDropdownMenu>
                </div>

                <div class="flex items-end justify-between gap-3">
                  <div>
                    <p class="text-xs text-muted">
                      {{ $t('realized') }}
                    </p>
                    <p class="text-xl font-semibold">
                      {{ formatCurrency(realizedFor(goal)) }}
                    </p>
                  </div>
                  <p class="text-sm text-muted">
                    {{ $t('expected') }} {{ formatCurrency(expectedFor(goal)) }}
                  </p>
                </div>

                <UProgress :model-value="expectedFor(goal) > 0 ? Math.min(realizedFor(goal) / expectedFor(goal) * 100, 100) : 0" />
                <div class="flex items-center justify-between gap-3">
                  <span class="text-sm text-muted">
                    {{ expectedFor(goal) > 0 ? `${Math.round(realizedFor(goal) / expectedFor(goal) * 100)}%` : '0%' }}
                  </span>
                  <UButton size="sm" variant="outline" icon="i-lucide-plus" :label="$t('recordRealization')" @click="openContribution(goal)" />
                </div>

                <div v-if="objectiveFor(goal)" class="space-y-1 border-t border-default pt-3">
                  <div class="flex items-center justify-between gap-3 text-sm">
                    <span class="text-muted">{{ $t('objective') }}</span>
                    <span class="font-medium">
                      {{ formatCurrency(savedFor(goal)) }} / {{ formatCurrency(objectiveFor(goal) || 0) }}
                    </span>
                  </div>
                  <UProgress :model-value="Math.min(objectiveProgressFor(goal), 100)" />
                  <p class="text-xs text-muted">
                    {{ $t('objectiveProgress', { value: Math.round(objectiveProgressFor(goal)) }) }}
                  </p>
                </div>
              </div>
            </UPageCard>
          </div>

          <UPageCard v-else variant="subtle" class="py-8 text-center">
            <UIcon name="i-lucide-target" class="mx-auto mb-3 size-8 text-muted" />
            <h2 class="font-semibold text-highlighted">
              {{ $t('noGoalsYet') }}
            </h2>
            <p class="mt-1 text-sm text-muted">
              {{ $t('createFirstGoal') }}
            </p>
            <UButton class="mt-4" icon="i-lucide-plus" :label="$t('newGoal')" @click="openGoalEditor()" />
          </UPageCard>

          <CategoryGoalReport :rows="categoryReport" :income="monthlyIncome" :loading="loading" />
        </div>

        <UModal v-model:open="newGoalOpen" :title="editingGoal ? $t('editGoal') : $t('newGoal')" :description="$t('newGoalDescription')">
          <template #body>
            <UForm :schema="createSchema" :state="createState" class="space-y-4" @submit="onCreateGoal">
              <UFormField :label="$t('goalName')" name="title" required>
                <UInput v-model="createState.title" class="w-full" :placeholder="$t('goalNameExample')" />
              </UFormField>
              <UFormField :label="$t('goalType')" name="mode" required>
                <USelect v-model="createState.mode" :items="modeItems" class="w-full" />
              </UFormField>
              <UFormField :label="createState.mode === 'FIXED' ? $t('monthlyAmount') : $t('incomePercent')" name="amount" required>
                <UInput v-model="createState.amount" type="number" min="0.01" :max="createState.mode === 'PERCENT_INCOME' ? 100 : undefined" step="0.01" class="w-full" />
              </UFormField>
              <div class="grid gap-4 sm:grid-cols-2">
                <UFormField
                  :label="$t('initialBalance')"
                  name="initialBalance"
                  :description="initialBalanceEditable ? $t('initialBalanceHelp') : $t('initialBalanceLocked')"
                >
                  <UInput
                    v-model="createState.initialBalance"
                    type="number"
                    min="0"
                    step="0.01"
                    class="w-full"
                    :disabled="!initialBalanceEditable"
                  />
                </UFormField>
                <UFormField :label="$t('objective')" name="objective" :description="$t('objectiveHelp')">
                  <UInput
                    v-model="createState.objective"
                    type="number"
                    min="0"
                    step="0.01"
                    class="w-full"
                  />
                </UFormField>
              </div>
              <div class="grid gap-4 sm:grid-cols-2">
                <UFormField :label="$t('startDate')" name="dtInicio" required>
                  <UInput v-model="createState.dtInicio" type="date" class="w-full" />
                </UFormField>
                <UFormField :label="$t('endDate')" name="dtFim" :description="$t('goalEndDateHelp')">
                  <UInput v-model="createState.dtFim" type="date" class="w-full" />
                </UFormField>
              </div>
              <p class="text-xs text-muted">
                {{ $t('goalAccountHelp', { account: activeAccount?.name || '' }) }}
              </p>
              <div class="flex justify-end gap-2">
                <UButton color="neutral" variant="subtle" :label="$t('cancel')" @click="newGoalOpen = false; editingGoal = null" />
                <UButton type="submit" :label="$t('save')" />
              </div>
            </UForm>
          </template>
        </UModal>

        <UModal v-model:open="contributionOpen" :title="$t('recordGoalRealization')" :description="selectedGoal?.title">
          <template #body>
            <UForm :schema="contributionSchema" :state="contributionState" class="space-y-4" @submit="onAddContribution">
              <UFormField :label="$t('amount')" name="amount" required>
                <UInput v-model="contributionState.amount" type="number" min="0.01" step="0.01" class="w-full" />
              </UFormField>
              <UFormField :label="$t('date')" name="date" required>
                <UInput v-model="contributionState.date" type="date" class="w-full" />
              </UFormField>
              <div class="flex justify-end gap-2">
                <UButton color="neutral" variant="subtle" :label="$t('cancel')" @click="contributionOpen = false" />
                <UButton type="submit" :label="$t('recordRealization')" />
              </div>
            </UForm>
          </template>
        </UModal>
      </template>
    </UDashboardPanel>
  </DashLayout>
</template>
