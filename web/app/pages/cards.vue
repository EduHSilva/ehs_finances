<script setup lang="ts">
import DashLayout from '~/layouts/DashLayout.vue'
import CustomHeader from '~/components/CustomHeader.vue'
import AddCardModal from '~/components/cards/AddCardModal.vue'
import CardInvoicesModal from '~/components/cards/CardInvoicesModal.vue'
import DeleteConfirmModal from '~/components/DeleteConfirmModal.vue'
import type { CreditCard } from '~/types'

const { $financesService } = useNuxtApp()
const { activeAccount, loadAccounts } = useFinancialAccounts()
const { cards, loading, error, loadCards } = useCreditCards()
const { t } = useI18n()
const toast = useToast()

const cardModalRef = ref<InstanceType<typeof AddCardModal> | null>(null)
const invoicesModalRef = ref<InstanceType<typeof CardInvoicesModal> | null>(null)
const deleteModalRef = ref<InstanceType<typeof DeleteConfirmModal> | null>(null)
const selectedCard = ref<CreditCard | null>(null)
const deleting = ref(false)

function showNewCard() {
  selectedCard.value = null
  cardModalRef.value?.openModal(null)
}

function openEditCard(card: CreditCard) {
  selectedCard.value = card
  cardModalRef.value?.openModal(card)
}

function openInvoices(card: CreditCard) {
  selectedCard.value = card
  invoicesModalRef.value?.openModal(card)
}

function cardMenuItems(card: CreditCard) {
  return [
    { label: t('viewInvoices'), icon: 'i-lucide-receipt-text', onSelect: () => openInvoices(card) },
    { label: t('edit'), icon: 'i-lucide-edit', onSelect: () => openEditCard(card) },
    { label: t('delete'), icon: 'i-lucide-trash', color: 'error' as const, onSelect: () => askDelete(card) }
  ]
}

function askDelete(card: CreditCard) {
  deleteModalRef.value?.openModal({
    title: t('deleteCardTitle'),
    description: t('deleteCardDesc', { name: card.name }),
    submitLabel: t('delete'),
    onConfirm: () => removeCard(card)
  })
}

async function removeCard(card: CreditCard) {
  deleting.value = true
  try {
    await $financesService.deleteCard(card.id)
    toast.add({ title: t('cardDeleted'), color: 'success', icon: 'i-lucide-check' })
    await refresh()
  } catch {
    toast.add({ title: t('cardDeleteFailed'), color: 'error' })
  } finally {
    deleting.value = false
  }
}

function usage(card: CreditCard) {
  if (!card.cardLimit || card.cardLimit <= 0) return null
  return Math.min(100, Math.round((card.openInvoiceTotal / card.cardLimit) * 100))
}

function usageColor(percent: number) {
  if (percent >= 90) return 'bg-error'
  if (percent >= 70) return 'bg-warning'
  return 'bg-primary'
}

async function refresh() {
  await loadCards()
}

watch(() => activeAccount.value?.id, refresh)
onMounted(() => loadAccounts().then(refresh))
</script>

<template>
  <DashLayout>
    <UDashboardPanel id="cards">
      <template #header>
        <CustomHeader>
          <UButton
            v-if="activeAccount"
            :label="$t('newCard')"
            icon="i-lucide-plus"
            class="bg-primary dark:bg-primary-dark"
            @click="showNewCard"
          />
        </CustomHeader>
      </template>

      <template #body>
        <AddCardModal
          ref="cardModalRef"
          :refresh="refresh"
        />
        <CardInvoicesModal
          ref="invoicesModalRef"
          @changed="refresh"
        />
        <DeleteConfirmModal
          ref="deleteModalRef"
          :loading="deleting"
        />

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
          :title="$t('cardsLoadFailed')"
          :actions="[{ label: $t('retry'), onClick: refresh }]"
        />
        <div
          v-else-if="loading && !cards.length"
          class="grid gap-3 sm:grid-cols-2"
        >
          <USkeleton class="h-36 rounded-lg" />
          <USkeleton class="h-36 rounded-lg" />
        </div>
        <UPageCard
          v-else-if="!cards.length"
          variant="subtle"
        >
          <div class="flex flex-col items-center gap-3 py-10 text-center">
            <UIcon
              name="i-lucide-credit-card"
              class="size-8 text-muted"
            />
            <p class="text-sm text-muted">
              {{ $t('noCardsYet') }}
            </p>
            <UButton
              :label="$t('newCard')"
              icon="i-lucide-plus"
              @click="showNewCard"
            />
          </div>
        </UPageCard>
        <div
          v-else
          class="grid gap-3 sm:grid-cols-2"
        >
          <UPageCard
            v-for="card in cards"
            :key="card.id"
            variant="subtle"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <div class="flex items-center gap-2">
                  <UIcon
                    name="i-lucide-credit-card"
                    class="text-primary"
                  />
                  <p class="truncate font-medium text-highlighted">
                    {{ card.name }}
                  </p>
                </div>
                <p class="mt-1 text-xs text-muted">
                  {{ $t('closingDayShort', { day: card.closingDay }) }} ·
                  {{ $t('dueDayShort', { day: card.dueDay }) }}
                </p>
              </div>
              <UDropdownMenu
                :items="cardMenuItems(card)"
                :content="{ align: 'end' }"
              >
                <UButton
                  icon="i-lucide-more-vertical"
                  variant="ghost"
                  color="neutral"
                  size="sm"
                  :aria-label="$t('actions')"
                />
              </UDropdownMenu>
            </div>

            <dl class="mt-4 space-y-3 text-sm">
              <div class="flex items-baseline justify-between gap-4">
                <dt class="text-muted">
                  {{ $t('openInvoices') }}
                </dt>
                <dd class="font-semibold tabular-nums">
                  {{ formatCurrency(card.openInvoiceTotal) }}
                </dd>
              </div>
              <div class="flex items-baseline justify-between gap-4">
                <dt class="text-muted">
                  {{ $t('cardLimit') }}
                </dt>
                <dd class="tabular-nums">
                  {{ card.cardLimit ? formatCurrency(card.cardLimit) : '—' }}
                </dd>
              </div>
            </dl>

            <div
              v-if="usage(card) !== null"
              class="mt-3"
            >
              <div class="flex items-center justify-between text-xs text-muted">
                <span>{{ $t('limitUsage') }}</span>
                <span class="tabular-nums">{{ usage(card) }}%</span>
              </div>
              <div class="mt-1 h-1.5 overflow-hidden rounded-full bg-elevated">
                <div
                  class="h-full rounded-full"
                  :class="usageColor(usage(card)!)"
                  :style="{ width: `${usage(card)}%` }"
                />
              </div>
            </div>

            <UButton
              class="mt-4 w-full justify-center"
              color="neutral"
              variant="outline"
              icon="i-lucide-receipt-text"
              :label="$t('viewInvoices')"
              @click="openInvoices(card)"
            />
          </UPageCard>
        </div>
      </template>
    </UDashboardPanel>
  </DashLayout>
</template>
