<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'
import type { CreditCard } from '~/types'

const props = defineProps({
  refresh: { type: Function, required: true }
})

const { $financesService } = useNuxtApp()
const { activeAccount } = useFinancialAccounts()
const { t } = useI18n()
const toast = useToast()
const open = ref(false)
const saving = ref(false)

const schema = z.object({
  id: z.string().optional(),
  name: z.string().trim().min(2, 'Too short').max(80),
  cardLimit: z.union([z.coerce.number().positive(), z.literal(''), z.null(), z.undefined()]).optional(),
  initialBalance: z.union([z.coerce.number().positive(), z.literal(''), z.null(), z.undefined()]).optional(),
  closingDay: z.coerce.number().int().min(1).max(31),
  dueDay: z.coerce.number().int().min(1).max(31)
})

type Schema = z.output<typeof schema>

const state = reactive<Partial<Schema>>({
  id: undefined,
  name: undefined,
  cardLimit: undefined,
  initialBalance: undefined,
  closingDay: undefined,
  dueDay: undefined
})

function resetState() {
  Object.assign(state, {
    id: undefined,
    name: undefined,
    cardLimit: undefined,
    initialBalance: undefined,
    closingDay: undefined,
    dueDay: undefined
  })
}

function applyCard(card: CreditCard) {
  Object.assign(state, {
    id: card.id,
    name: card.name,
    cardLimit: card.cardLimit ?? undefined,
    closingDay: card.closingDay,
    dueDay: card.dueDay
  })
}

function normalizeLimit(value: Schema['cardLimit']) {
  if (value === '' || value == null) return null
  return Number(value)
}

async function onSubmit(event: FormSubmitEvent<Schema>) {
  if (!activeAccount.value) return
  const payload = {
    accountId: activeAccount.value.id,
    name: event.data.name,
    cardLimit: normalizeLimit(event.data.cardLimit),
    closingDay: event.data.closingDay,
    dueDay: event.data.dueDay
  }
  saving.value = true
  try {
    if (event.data.id) {
      await $financesService.updateCard(event.data.id, payload)
      toast.add({ title: t('cardUpdated'), color: 'success', icon: 'i-lucide-check' })
    } else {
      await $financesService.createCard({
        ...payload,
        initialBalance: normalizeLimit(event.data.initialBalance)
      })
      toast.add({ title: t('cardCreated'), color: 'success', icon: 'i-lucide-check' })
    }
    open.value = false
    resetState()
    await props.refresh()
  } catch {
    toast.add({
      title: event.data.id ? t('cardSaveFailed') : t('cardCreateFailed'),
      color: 'error'
    })
  } finally {
    saving.value = false
  }
}

function openModal(card: CreditCard | null) {
  if (card) applyCard(card)
  else resetState()
  open.value = true
}

function closeModal() {
  open.value = false
  resetState()
}

defineExpose({ openModal, closeModal })
</script>

<template>
  <UModal
    v-model:open="open"
    :title="state.id ? $t('editCard') : $t('newCard')"
    :description="$t('cardModalDesc')"
  >
    <template #body>
      <UForm
        :schema="schema"
        :state="state"
        class="space-y-4"
        @submit="onSubmit"
      >
        <UFormField
          :label="$t('name')"
          name="name"
          required
        >
          <UInput
            v-model="state.name"
            class="w-full"
            :placeholder="$t('cardNameExample')"
          />
        </UFormField>

        <UFormField
          :label="$t('cardLimit')"
          name="cardLimit"
          :help="$t('cardLimitHelp')"
        >
          <UInput
            v-model="state.cardLimit"
            class="w-full"
            type="number"
            min="0"
            step="0.01"
            :placeholder="$t('optional')"
          />
        </UFormField>

        <UFormField
          v-if="!state.id"
          :label="$t('cardInitialBalance')"
          name="initialBalance"
          :help="$t('cardInitialBalanceHelp')"
        >
          <UInput
            v-model="state.initialBalance"
            class="w-full"
            type="number"
            min="0"
            step="0.01"
            :placeholder="$t('optional')"
          />
        </UFormField>

        <div class="flex items-center gap-2">
          <UFormField
            :label="$t('closingDay')"
            name="closingDay"
            required
            class="flex-1"
          >
            <UInput
              v-model="state.closingDay"
              type="number"
              min="1"
              max="31"
              class="w-full"
            />
          </UFormField>

          <UFormField
            :label="$t('dueDay')"
            name="dueDay"
            required
            class="flex-1"
          >
            <UInput
              v-model="state.dueDay"
              type="number"
              min="1"
              max="31"
              class="w-full"
            />
          </UFormField>
        </div>

        <p class="text-xs text-muted">
          {{ $t('cardCycleHelp') }}
        </p>

        <div class="flex justify-end gap-2">
          <UButton
            :label="$t('cancel')"
            color="neutral"
            variant="subtle"
            :disabled="saving"
            @click="closeModal"
          />
          <UButton
            :label="$t('save')"
            color="primary"
            variant="solid"
            type="submit"
            :loading="saving"
          />
        </div>
      </UForm>
    </template>
  </UModal>
</template>
