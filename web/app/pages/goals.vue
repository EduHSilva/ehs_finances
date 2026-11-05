<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'
import type { MonthlyGoal, MonthlyGoalMode } from '~/types'
import type { MonthlyGoalRequest } from '~/services/financesService'
import DashLayout from '~/layouts/DashLayout.vue'
import CustomHeader from '~/components/CustomHeader.vue'

const { activeAccount, loadAccounts } = useFinancialAccounts()
const { $financesService } = useNuxtApp()
const { goals, summaries, loading, load, create, update, remove, contribute } = useMonthlyGoals()
const toast = useToast()
const { t, locale } = useI18n()
const currentMonth = ref(new Date())
const monthlyIncome = ref(0)
const newGoalOpen = ref(false)
const contributionOpen = ref(false)
const selectedGoal = ref<MonthlyGoal | null>(null)
const editingGoal = ref<MonthlyGoal | null>(null)

const createSchema = z.object({
  title: z.string().trim().min(2),
  mode: z.enum(['FIXED', 'PERCENT_INCOME']),
  amount: z.coerce.number().positive()
}).refine(value => value.mode !== 'PERCENT_INCOME' || value.amount <= 100, {
  path: ['amount'],
  message: t('percentageMaximum')
})
const contributionSchema = z.object({
  amount: z.coerce.number().positive(),
  date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/)
})

type CreateSchema = z.output<typeof createSchema>
type ContributionSchema = z.output<typeof contributionSchema>

const createState = reactive<Partial<CreateSchema>>({ title: '', mode: 'FIXED', amount: undefined })
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

const expectedTotal = computed(() => accountGoals.value.reduce((total, goal) => total + expectedFor(goal), 0))
const realizedTotal = computed(() => accountGoals.value.reduce((total, goal) => total + realizedFor(goal), 0))

let refreshSequence = 0
async function refreshGoals() {
  const account = activeAccount.value
  if (!account) return
  const sequence = ++refreshSequence
  await load(account.id, monthKey.value)
  if (sequence !== refreshSequence || activeAccount.value?.id !== account.id) return
  if (goals.value.length) {
    monthlyIncome.value = summaries.value[goals.value[0]!.id]?.incomeBase || 0
    return
  }
  const transactions = await $financesService.getTransactions(account.id, 0, 200)
  if (sequence !== refreshSequence || activeAccount.value?.id !== account.id) return
  monthlyIncome.value = transactions.data.items
    .filter((transaction) => {
      if (!transaction.income || transaction.status !== 'OK' || !transaction.executionDate) return false
      const date = new Date(transaction.executionDate)
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}` === monthKey.value
    })
    .reduce((total, transaction) => total + transaction.value, 0)
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

function openGoalEditor(goal?: MonthlyGoal) {
  editingGoal.value = goal || null
  Object.assign(createState, goal
    ? { title: goal.title, mode: goal.mode, amount: goal.amount }
    : { title: '', mode: 'FIXED', amount: undefined })
  newGoalOpen.value = true
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
    periodMonth: monthKey.value,
    active: true
  }
  try {
    if (editingGoal.value) await update(editingGoal.value.id, request, account.id, monthKey.value)
    else await create(request, account.id, monthKey.value)
    newGoalOpen.value = false
    editingGoal.value = null
    Object.assign(createState, { title: '', mode: 'FIXED', amount: undefined })
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
              <p class="text-2xl font-semibold">{{ formatCurrency(expectedTotal) }}</p>
            </UPageCard>
            <UPageCard variant="subtle" :title="$t('realizedThisMonth')">
              <p class="text-2xl font-semibold text-primary">{{ formatCurrency(realizedTotal) }}</p>
            </UPageCard>
            <UPageCard variant="subtle" :title="$t('monthlyIncomeBase')">
              <p class="text-2xl font-semibold">{{ formatCurrency(monthlyIncome) }}</p>
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
                    <h2 class="font-semibold text-highlighted">{{ goal.title }}</h2>
                    <p class="text-sm text-muted">
                      {{ goal.mode === 'FIXED' ? $t('fixedMonthlyAmount') : `${goal.amount}% ${$t('ofIncome')}` }}
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
                    <p class="text-xs text-muted">{{ $t('realized') }}</p>
                    <p class="text-xl font-semibold">{{ formatCurrency(realizedFor(goal)) }}</p>
                  </div>
                  <p class="text-sm text-muted">{{ $t('expected') }} {{ formatCurrency(expectedFor(goal)) }}</p>
                </div>

                <UProgress :model-value="expectedFor(goal) > 0 ? Math.min(realizedFor(goal) / expectedFor(goal) * 100, 100) : 0" />
                <div class="flex items-center justify-between gap-3">
                  <span class="text-sm text-muted">
                    {{ expectedFor(goal) > 0 ? `${Math.round(realizedFor(goal) / expectedFor(goal) * 100)}%` : '0%' }}
                  </span>
                  <UButton size="sm" variant="outline" icon="i-lucide-plus" :label="$t('recordRealization')" @click="openContribution(goal)" />
                </div>
              </div>
            </UPageCard>
          </div>

          <UPageCard v-else variant="subtle" class="py-8 text-center">
            <UIcon name="i-lucide-target" class="mx-auto mb-3 size-8 text-muted" />
            <h2 class="font-semibold text-highlighted">{{ $t('noGoalsYet') }}</h2>
            <p class="mt-1 text-sm text-muted">{{ $t('createFirstGoal') }}</p>
            <UButton class="mt-4" icon="i-lucide-plus" :label="$t('newGoal')" @click="openGoalEditor()" />
          </UPageCard>
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
              <p class="text-xs text-muted">{{ $t('goalAccountHelp', { account: activeAccount?.name || '' }) }}</p>
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
