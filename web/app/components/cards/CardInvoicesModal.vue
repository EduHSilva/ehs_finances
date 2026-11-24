<script setup lang="ts">
import type { CreditCard, CreditCardInvoice, CreditCardInvoicePayment, Transaction } from '~/types'
import DeleteConfirmModal from '~/components/DeleteConfirmModal.vue'
import { invoiceCycle, localIsoDate, openInvoiceMonth } from '~/composables/useCreditCards'

const props = defineProps({
  refreshTransactions: { type: Function, default: undefined }
})

const emit = defineEmits<{
  (e: 'changed'): void
}>()

const { $financesService } = useNuxtApp()
const { activeAccount } = useFinancialAccounts()
const { t, locale } = useI18n()
const numberLocale = computed(() => locale.value === 'pt' ? 'pt-BR' : 'en-US')
const toast = useToast()

const open = ref(false)
const card = ref<CreditCard | null>(null)
const invoices = ref<CreditCardInvoice[]>([])
const transactions = ref<Transaction[]>([])
const selectedMonth = ref<string | null>(null)
const loading = ref(false)
const error = ref(false)
const saving = ref(false)

const paymentState = reactive<{ amount?: number, date?: Date }>({ amount: undefined, date: undefined })
const paymentOpen = ref(false)

const deletePaymentRef = ref<InstanceType<typeof DeleteConfirmModal> | null>(null)

const selectedInvoice = computed(() =>
  invoices.value.find(invoice => invoice.invoiceMonth === selectedMonth.value) || null
)

const cycle = computed(() => {
  if (!card.value || !selectedMonth.value) return null
  return invoiceCycle(card.value.closingDay, card.value.dueDay, selectedMonth.value)
})

const cyclePurchases = computed(() => {
  if (!card.value || !cycle.value) return []
  const { startDate, closingDate } = cycle.value
  return transactions.value
    .filter((transaction) => {
      if (transaction.creditCardId !== card.value?.id) return false
      if (transaction.entryType && transaction.entryType !== 'CARD') return false
      const date = transaction.executionDate.slice(0, 10)
      return date >= startDate && date <= closingDate
    })
    .sort((a, b) => b.executionDate.localeCompare(a.executionDate))
})

const unpaidPurchases = computed(() =>
  cyclePurchases.value.filter(transaction => transaction.status === 'PENDING')
)

const canPayInvoice = computed(() => {
  const invoice = selectedInvoice.value
  return !!invoice && invoice.status !== 'PAID' && invoice.remaining > 0 && !unpaidPurchases.value.length
})

function statusColor(status: CreditCardInvoice['status']) {
  return {
    OPEN: 'warning' as const,
    PARTIAL: 'primary' as const,
    PAID: 'success' as const
  }[status]
}

function invoicesTotal() {
  return invoices.value.reduce((total, invoice) => total + invoice.total, 0)
}

async function loadInvoices(id: string) {
  const response = await $financesService.getCardInvoices(id)
  invoices.value = response.data || []
}

async function loadTransactions(accountId: string) {
  const all: Transaction[] = []
  let page = 0
  const size = 200
  let total = Infinity
  while (all.length < total) {
    const response = await $financesService.getTransactions(accountId, page, size)
    all.push(...response.data.items as Transaction[])
    total = response.data.total
    if (!response.data.items.length) break
    page++
  }
  transactions.value = all
}

async function reload() {
  if (!card.value) return
  loading.value = true
  error.value = false
  try {
    await loadInvoices(card.value.id)
    if (activeAccount.value) await loadTransactions(activeAccount.value.id)
    if (selectedMonth.value && !invoices.value.some(invoice => invoice.invoiceMonth === selectedMonth.value)) {
      selectedMonth.value = invoices.value[0]?.invoiceMonth ?? selectedMonth.value
    }
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function openModal(selected: CreditCard) {
  card.value = selected
  selectedMonth.value = openInvoiceMonth(selected, localIsoDate(new Date()))
  invoices.value = []
  transactions.value = []
  open.value = true
  reload()
}

function closeModal() {
  open.value = false
  card.value = null
  selectedMonth.value = null
}

function openPayment() {
  if (!selectedInvoice.value) return
  paymentState.amount = selectedInvoice.value.remaining
  paymentState.date = parseLocalDate(localIsoDate(new Date()))
  paymentOpen.value = true
}

async function submitPayment() {
  if (!card.value || !selectedInvoice.value) return
  const value = Number(paymentState.amount)
  if (!Number.isFinite(value) || value <= 0) {
    toast.add({ title: t('confirmValueInvalid'), color: 'error' })
    return
  }
  if (value > selectedInvoice.value.remaining) {
    toast.add({ title: t('invoicePaymentExceeds'), color: 'error' })
    return
  }
  saving.value = true
  try {
    const date = paymentState.date
      ? toExecutionDate(paymentState.date).slice(0, 10)
      : localIsoDate(new Date())
    await $financesService.payInvoice(card.value.id, selectedInvoice.value.invoiceMonth, { value, date })
    toast.add({ title: t('invoicePaid'), color: 'success', icon: 'i-lucide-check' })
    paymentOpen.value = false
    await reload()
    await props.refreshTransactions?.()
    emit('changed')
  } catch {
    toast.add({ title: t('invoicePayFailed'), color: 'error' })
  } finally {
    saving.value = false
  }
}

function askDeletePayment(payment: CreditCardInvoicePayment) {
  if (!card.value || !selectedInvoice.value) return
  const cardId = card.value.id
  const invoiceMonth = selectedInvoice.value.invoiceMonth
  deletePaymentRef.value?.openModal({
    title: t('deleteInvoicePaymentTitle'),
    description: t('deleteInvoicePaymentDesc', { value: formatCurrency(payment.value, locale.value) }),
    submitLabel: t('delete'),
    onConfirm: async () => {
      try {
        await $financesService.deleteInvoicePayment(cardId, invoiceMonth, payment.id)
        toast.add({ title: t('invoicePaymentDeleted'), color: 'success', icon: 'i-lucide-check' })
        await reload()
        await props.refreshTransactions?.()
        emit('changed')
      } catch {
        toast.add({ title: t('invoicePaymentDeleteFailed'), color: 'error' })
      }
    }
  })
}

const invoiceOptions = computed(() => invoices.value.map(invoice => ({
  label: invoiceLabel(invoice),
  value: invoice.invoiceMonth
})))

function invoiceLabel(invoice: CreditCardInvoice) {
  const [year, month] = invoice.invoiceMonth.split('-').map(Number)
  const label = new Intl.DateTimeFormat(numberLocale.value, { month: 'long', year: 'numeric' })
    .format(new Date(year || 2000, (month || 1) - 1, 1))
  return `${label.charAt(0).toUpperCase()}${label.slice(1)} · ${formatCurrency(invoice.total)}`
}

defineExpose({ openModal, closeModal })
</script>

<template>
  <UModal
    v-model:open="open"
    :title="card ? card.name : $t('invoices')"
    :description="card ? $t('invoicesDesc') : undefined"
    :ui="{ content: 'sm:max-w-3xl' }"
  >
    <template #body>
      <div
        v-if="loading"
        class="space-y-3"
      >
        <USkeleton class="h-10 rounded-lg" />
        <USkeleton class="h-28 rounded-lg" />
        <USkeleton class="h-40 rounded-lg" />
      </div>
      <UAlert
        v-else-if="error"
        color="error"
        variant="subtle"
        icon="i-lucide-triangle-alert"
        :title="$t('invoicesLoadFailed')"
        :actions="[{ label: $t('retry'), onClick: reload }]"
      />
      <div
        v-else
        class="space-y-4"
      >
        <div class="flex flex-wrap items-end justify-between gap-3">
          <UFormField
            :label="$t('invoiceCycle')"
            class="min-w-56 flex-1"
          >
            <USelect
              :items="invoiceOptions"
              :model-value="selectedMonth || undefined"
              :placeholder="$t('selectInvoice')"
              class="w-full"
              @update:model-value="value => selectedMonth = value || null"
            />
          </UFormField>
          <div class="flex flex-wrap items-center gap-2">
            <UButton
              :label="$t('payInvoice')"
              icon="i-lucide-banknote"
              :disabled="!canPayInvoice"
              @click="openPayment"
            />
          </div>
        </div>

        <UAlert
          v-if="unpaidPurchases.length"
          color="warning"
          variant="subtle"
          icon="i-lucide-clock"
          :title="$t('invoicePendingPurchases', { count: unpaidPurchases.length })"
          :description="$t('invoicePendingPurchasesHelp')"
        />

        <div v-if="selectedInvoice">
          <div class="grid gap-3 sm:grid-cols-4">
            <div class="rounded-lg border border-default p-3">
              <p class="text-xs text-muted">
                {{ $t('invoiceStatusLabel') }}
              </p>
              <UBadge
                class="mt-1"
                variant="subtle"
                :color="statusColor(selectedInvoice.status)"
              >
                {{ $t(`invoiceStatus.${selectedInvoice.status.toLowerCase()}`) }}
              </UBadge>
            </div>
            <div class="rounded-lg border border-default p-3">
              <p class="text-xs text-muted">
                {{ $t('invoiceTotal') }}
              </p>
              <p class="mt-1 font-semibold tabular-nums">
                {{ formatCurrency(selectedInvoice.total) }}
              </p>
            </div>
            <div class="rounded-lg border border-default p-3">
              <p class="text-xs text-muted">
                {{ $t('invoicePaid') }}
              </p>
              <p class="mt-1 font-semibold tabular-nums text-success">
                {{ formatCurrency(selectedInvoice.paid) }}
              </p>
            </div>
            <div class="rounded-lg border border-default p-3">
              <p class="text-xs text-muted">
                {{ $t('invoiceRemaining') }}
              </p>
              <p
                class="mt-1 font-semibold tabular-nums"
                :class="selectedInvoice.remaining > 0 ? 'text-error' : 'text-highlighted'"
              >
                {{ formatCurrency(selectedInvoice.remaining) }}
              </p>
            </div>
          </div>

          <p class="mt-2 text-xs text-muted">
            {{ $t('invoiceCycleRange', {
              start: cycle ? formatDate(cycle.startDate) : '—',
              end: cycle ? formatDate(cycle.closingDate) : '—'
            }) }} ·
            {{ $t('invoiceDueDate', { date: formatDate(selectedInvoice.dueDate) }) }}
          </p>
        </div>
        <p
          v-else
          class="text-sm text-muted"
        >
          {{ $t('invoiceCyclePreview') }}
        </p>

        <div
          v-if="selectedMonth && cycle"
          class="space-y-2"
        >
          <div class="flex items-center justify-between">
            <p class="text-sm font-medium">
              {{ $t('invoicePurchases') }}
            </p>
            <span class="text-sm tabular-nums text-muted">
              {{ formatCurrency(cyclePurchases.reduce((total, item) => total + item.value, 0)) }}
            </span>
          </div>
          <div
            v-if="cyclePurchases.length"
            class="max-h-64 divide-y divide-default overflow-y-auto rounded-lg border border-default"
          >
            <div
              v-for="purchase in cyclePurchases"
              :key="purchase.id"
              class="flex items-center justify-between gap-3 px-3 py-2"
            >
              <div class="min-w-0">
                <p class="truncate text-sm font-medium">
                  {{ purchase.title }}
                </p>
                <p class="text-xs text-muted">
                  {{ formatDate(purchase.executionDate) }}
                  <template v-if="purchase.status !== 'OK'">
                    · {{ $t(`transactionStatus.${purchase.status.toLowerCase()}`) }}
                  </template>
                </p>
              </div>
              <span class="shrink-0 text-sm tabular-nums">{{ formatCurrency(purchase.value) }}</span>
            </div>
          </div>
          <p
            v-else
            class="rounded-lg border border-default py-6 text-center text-sm text-muted"
          >
            {{ $t('invoiceNoPurchases') }}
          </p>
        </div>

        <div
          v-if="selectedInvoice?.payments?.length"
          class="space-y-2"
        >
          <p class="text-sm font-medium">
            {{ $t('invoicePayments') }}
          </p>
          <div class="divide-y divide-default rounded-lg border border-default">
            <div
              v-for="payment in selectedInvoice.payments"
              :key="payment.id"
              class="flex items-center justify-between gap-3 px-3 py-2"
            >
              <div>
                <p class="text-sm font-medium tabular-nums">
                  {{ formatCurrency(payment.value) }}
                </p>
                <p class="text-xs text-muted">
                  {{ formatDate(payment.date) }}
                </p>
              </div>
              <UButton
                icon="i-lucide-trash"
                color="error"
                variant="ghost"
                size="xs"
                :disabled="saving"
                :aria-label="$t('deletePayment')"
                @click="askDeletePayment(payment)"
              />
            </div>
          </div>
        </div>

        <div
          v-if="selectedInvoice"
          class="mt-3 flex flex-wrap items-center justify-between gap-2 rounded-lg bg-elevated/50 px-3 py-2 text-xs text-muted"
        >
          <span>{{ $t('invoiceListSummary', { count: invoices.length }) }}</span>
          <span class="tabular-nums">{{ $t('invoicesTotalAll') }}: {{ formatCurrency(invoicesTotal()) }}</span>
        </div>
      </div>
    </template>
  </UModal>

  <DeleteConfirmModal ref="deletePaymentRef" />

  <UModal
    :open="paymentOpen"
    :title="$t('payInvoice')"
    :description="selectedInvoice ? `${selectedMonth} · ${$t('invoiceRemaining')}: ${formatCurrency(selectedInvoice.remaining)}` : undefined"
    @update:open="value => { paymentOpen = value }"
  >
    <template #body>
      <form
        class="space-y-4"
        @submit.prevent="submitPayment"
      >
        <UFormField
          :label="$t('value')"
          required
        >
          <UInput
            v-model="paymentState.amount"
            type="number"
            min="0.01"
            step="0.01"
            class="w-full"
          />
        </UFormField>
        <UFormField
          :label="$t('date')"
          required
        >
          <DateField
            v-model="paymentState.date"
            :locale="numberLocale"
          />
        </UFormField>
        <p class="text-xs text-muted">
          {{ $t('invoicePaymentHelp') }}
        </p>
        <div class="flex justify-end gap-2">
          <UButton
            :label="$t('cancel')"
            color="neutral"
            variant="subtle"
            :disabled="saving"
            @click="paymentOpen = false"
          />
          <UButton
            :label="$t('payInvoice')"
            type="submit"
            :loading="saving"
          />
        </div>
      </form>
    </template>
  </UModal>
</template>
