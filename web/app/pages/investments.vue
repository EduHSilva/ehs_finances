<script setup lang="ts">
import type { InvestmentMovement, InvestmentMovementType, InvestmentSummary } from '~/types'
import DashLayout from '~/layouts/DashLayout.vue'
import CustomHeader from '~/components/CustomHeader.vue'

const { activeAccount, loadAccounts } = useFinancialAccounts()
const { $financesService } = useNuxtApp()
const { t } = useI18n()
const toast = useToast()
const movements = ref<InvestmentMovement[]>([])
const summary = ref<InvestmentSummary | null>(null)
const loading = ref(false)
const saving = ref(false)
const modalOpen = ref(false)

const state = reactive<{
  type: InvestmentMovementType
  amount?: number
  date: string
  description: string
}>({
  type: 'CONTRIBUTION',
  amount: undefined,
  date: new Date().toISOString().slice(0, 10),
  description: ''
})

const typeItems = computed(() => [
  { label: t('contribution'), value: 'CONTRIBUTION' as InvestmentMovementType },
  { label: t('withdrawal'), value: 'WITHDRAWAL' as InvestmentMovementType }
])

async function refresh() {
  if (!activeAccount.value) {
    movements.value = []
    summary.value = null
    return
  }
  loading.value = true
  try {
    const accountId = activeAccount.value.id
    const [movementResponse, summaryResponse] = await Promise.all([
      $financesService.getInvestments(accountId),
      $financesService.getInvestmentSummary(accountId)
    ])
    movements.value = movementResponse.data
    summary.value = summaryResponse.data
  } catch {
    toast.add({ title: t('investmentLoadFailed'), color: 'error' })
  } finally {
    loading.value = false
  }
}

watch(() => activeAccount.value?.id, refresh, { immediate: true })
onMounted(() => loadAccounts().catch(() => {
  toast.add({ title: t('accountsLoadFailed'), color: 'error' })
}))

function openModal() {
  state.type = 'CONTRIBUTION'
  state.amount = undefined
  state.date = new Date().toISOString().slice(0, 10)
  state.description = ''
  modalOpen.value = true
}

async function saveMovement() {
  if (!activeAccount.value || !state.amount || state.amount <= 0) return
  saving.value = true
  try {
    await $financesService.createInvestmentMovement({
      accountId: activeAccount.value.id,
      type: state.type,
      amount: state.amount,
      date: state.date,
      description: state.description || undefined
    })
    modalOpen.value = false
    toast.add({ title: t('investmentMovementCreated'), color: 'success', icon: 'i-lucide-check' })
    await refresh()
  } catch {
    toast.add({ title: t('investmentSaveFailed'), color: 'error' })
  } finally {
    saving.value = false
  }
}

async function removeMovement(movement: InvestmentMovement) {
  try {
    await $financesService.deleteInvestmentMovement(movement.id)
    await refresh()
  } catch {
    toast.add({ title: t('investmentDeleteFailed'), color: 'error' })
  }
}
</script>

<template>
  <DashLayout>
    <UDashboardPanel id="investments">
      <template #header>
        <CustomHeader>
          <UButton
            v-if="activeAccount"
            :label="$t('newInvestmentMovement')"
            icon="i-lucide-plus"
            class="bg-primary dark:bg-primary-dark"
            @click="openModal"
          />
        </CustomHeader>
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
            <UPageCard variant="subtle" :title="$t('investmentBalance')">
              <p class="text-2xl font-semibold">{{ formatCurrency(summary?.balance || 0) }}</p>
            </UPageCard>
            <UPageCard variant="subtle" :title="$t('contributions')">
              <p class="text-2xl font-semibold text-success">{{ formatCurrency(summary?.totalContributions || 0) }}</p>
            </UPageCard>
            <UPageCard variant="subtle" :title="$t('withdrawals')">
              <p class="text-2xl font-semibold text-error">{{ formatCurrency(summary?.totalWithdrawals || 0) }}</p>
            </UPageCard>
          </div>

          <UPageCard variant="subtle" :title="$t('investmentMovements')">
            <div v-if="loading" class="space-y-3">
              <USkeleton v-for="index in 3" :key="index" class="h-12 rounded-lg" />
            </div>
            <div v-else-if="movements.length" class="divide-y divide-default">
              <div v-for="movement in movements" :key="movement.id" class="flex items-center justify-between gap-4 py-3">
                <div>
                  <p class="font-medium text-highlighted">
                    {{ movement.description || $t(movement.type === 'CONTRIBUTION' ? 'contribution' : 'withdrawal') }}
                  </p>
                  <p class="text-sm text-muted">
                    {{ formatDate(movement.date) }} · {{ $t(movement.type === 'CONTRIBUTION' ? 'contribution' : 'withdrawal') }}
                  </p>
                </div>
                <div class="flex items-center gap-3">
                  <span :class="movement.type === 'CONTRIBUTION' ? 'text-success' : 'text-error'">
                    {{ movement.type === 'CONTRIBUTION' ? '+' : '-' }}{{ formatCurrency(movement.amount) }}
                  </span>
                  <UButton
                    icon="i-lucide-trash"
                    color="error"
                    variant="ghost"
                    size="sm"
                    :aria-label="$t('delete')"
                    @click="removeMovement(movement)"
                  />
                </div>
              </div>
            </div>
            <p v-else class="py-6 text-center text-sm text-muted">{{ $t('noInvestments') }}</p>
          </UPageCard>
        </div>

        <UModal v-model:open="modalOpen" :title="$t('newInvestmentMovement')" :description="$t('newInvestmentMovementDesc')">
          <template #body>
            <div class="space-y-4">
              <UFormField :label="$t('movementType')" required>
                <USelect v-model="state.type" :items="typeItems" class="w-full" />
              </UFormField>
              <UFormField :label="$t('amount')" required>
                <UInput v-model="state.amount" type="number" min="0.01" step="0.01" class="w-full" />
              </UFormField>
              <UFormField :label="$t('date')" required>
                <UInput v-model="state.date" type="date" class="w-full" />
              </UFormField>
              <UFormField :label="$t('description')">
                <UInput v-model="state.description" class="w-full" :placeholder="$t('optional')" />
              </UFormField>
              <div class="flex justify-end gap-2">
                <UButton :label="$t('cancel')" color="neutral" variant="subtle" @click="modalOpen = false" />
                <UButton :label="$t('save')" :loading="saving" @click="saveMovement" />
              </div>
            </div>
          </template>
        </UModal>
      </template>
    </UDashboardPanel>
  </DashLayout>
</template>
