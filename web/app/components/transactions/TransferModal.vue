<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'

interface TransferEditTarget {
  transferId: string
  fromAccountId: string
  toAccountId: string
  value: number
  date: string
  title?: string | null
}

const props = defineProps({
  refresh: { type: Function, required: true }
})

const { $financesService } = useNuxtApp()
const { visibleAccounts, activeAccount, loadAccounts } = useFinancialAccounts()
const { t, locale } = useI18n()
const localeTag = computed(() => locale.value === 'pt' ? 'pt-BR' : 'en-US')
const toast = useToast()
const open = ref(false)
const saving = ref(false)
const editing = ref<TransferEditTarget | null>(null)

const schema = z.object({
  fromAccountId: z.string().min(1, 'Required'),
  toAccountId: z.string().min(1, 'Required'),
  value: z.coerce.number().positive(t('confirmValueInvalid')),
  date: z.any(),
  title: z.string().trim().max(80).optional()
}).superRefine((data, ctx) => {
  if (data.fromAccountId === data.toAccountId) {
    ctx.addIssue({ code: 'custom', path: ['toAccountId'], message: t('transferSameAccount') })
  }
})

type Schema = z.output<typeof schema>

const state = reactive<Partial<Schema>>({
  fromAccountId: undefined,
  toAccountId: undefined,
  value: undefined,
  date: undefined,
  title: undefined
})

const accountItems = computed(() => visibleAccounts.value.map(account => ({
  label: `${account.type} - ${account.name}`,
  value: account.id
})))

const destinationItems = computed(() =>
  accountItems.value.filter(account => account.value !== state.fromAccountId)
)

function resetState() {
  const origin = activeAccount.value?.id || visibleAccounts.value[0]?.id
  const destination = visibleAccounts.value.find(account => account.id !== origin)?.id
  Object.assign(state, {
    fromAccountId: origin,
    toAccountId: destination,
    value: undefined,
    date: parseLocalDate(localIsoDate(new Date())),
    title: undefined
  })
  editing.value = null
}

function applyEdit(target: TransferEditTarget) {
  editing.value = target
  Object.assign(state, {
    fromAccountId: target.fromAccountId,
    toAccountId: target.toAccountId,
    value: target.value,
    date: parseLocalDate(target.date),
    title: target.title || undefined
  })
}

watch(() => state.fromAccountId, (origin) => {
  if (origin && state.toAccountId === origin) {
    state.toAccountId = visibleAccounts.value.find(account => account.id !== origin)?.id
  }
})

async function onSubmit(event: FormSubmitEvent<Schema>) {
  const date = event.data.date ? toExecutionDate(event.data.date).slice(0, 10) : localIsoDate(new Date())
  saving.value = true
  try {
    if (editing.value) {
      await $financesService.updateTransfer(editing.value.transferId, {
        value: event.data.value,
        date,
        title: event.data.title || undefined
      })
      toast.add({ title: t('transferUpdated'), color: 'success', icon: 'i-lucide-check' })
    } else {
      await $financesService.createTransfer({
        fromAccountId: event.data.fromAccountId,
        toAccountId: event.data.toAccountId,
        value: event.data.value,
        date,
        title: event.data.title || undefined
      })
      toast.add({ title: t('transferCreated'), color: 'success', icon: 'i-lucide-check' })
    }
    open.value = false
    resetState()
    await loadAccounts(true)
    await props.refresh()
  } catch {
    toast.add({
      title: editing.value ? t('transferSaveFailed') : t('transferCreateFailed'),
      color: 'error'
    })
  } finally {
    saving.value = false
  }
}

function openModal(target: TransferEditTarget | null = null) {
  if (target) applyEdit(target)
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
    :title="editing ? $t('editTransfer') : $t('newTransfer')"
    :description="editing ? $t('editTransferDesc') : $t('newTransferDesc')"
  >
    <template #body>
      <UForm
        :schema="schema"
        :state="state"
        class="space-y-4"
        @submit="onSubmit"
      >
        <UFormField
          :label="$t('fromAccount')"
          name="fromAccountId"
          required
        >
          <USelect
            v-model="state.fromAccountId"
            :items="accountItems"
            class="w-full"
            :disabled="!!editing"
          />
        </UFormField>

        <UFormField
          :label="$t('toAccount')"
          name="toAccountId"
          required
        >
          <USelect
            v-model="state.toAccountId"
            :items="destinationItems"
            class="w-full"
            :disabled="!!editing"
          />
        </UFormField>

        <div class="flex items-start gap-2">
          <UFormField
            :label="$t('value')"
            name="value"
            required
            class="flex-1"
          >
            <UInput
              v-model="state.value"
              type="number"
              min="0.01"
              step="0.01"
              class="w-full"
            />
          </UFormField>
          <UFormField
            :label="$t('date')"
            name="date"
            required
            class="flex-1"
          >
            <UInputDate
              v-model="state.date"
              :locale="localeTag"
            />
          </UFormField>
        </div>

        <UFormField
          :label="$t('title')"
          name="title"
          :help="$t('transferTitleHelp')"
        >
          <UInput
            v-model="state.title"
            class="w-full"
            :placeholder="$t('optional')"
          />
        </UFormField>

        <p
          v-if="editing"
          class="text-xs text-muted"
        >
          {{ $t('transferEditScopeHelp') }}
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
            :disabled="!destinationItems.length"
          />
        </div>
      </UForm>
    </template>
  </UModal>
</template>
