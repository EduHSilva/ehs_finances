<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'
import type { Category, Transaction } from '~/types'

const props = defineProps({
  refresh: { type: Function, required: true },
  transaction: { type: Object as () => Transaction | null, default: null },
  categories: { type: Array as PropType<Category[]>, required: true }
})
const { activeAccount } = useFinancialAccounts()
const { locale, t } = useI18n()
const categoryLabel = useCategoryLabel()
const localeTag = computed(() => locale.value === 'pt' ? 'pt-BR' : 'en-US')
const toast = useToast()
const open = ref(false)

const schema = z.object({
  id: z.string().optional(),
  title: z.string().min(2, 'Too short'),
  value: z.coerce.number(),
  categoryID: z.string().optional(),
  income: z.boolean().default(false),
  pending: z.boolean().default(false),
  executionDate: z.any()
})

type Schema = z.output<typeof schema>

const state = reactive<Partial<Schema>>({
  title: undefined,
  id: undefined,
  value: undefined,
  categoryID: undefined,
  income: false,
  pending: false,
  executionDate: undefined
})

const canEditCategory = computed(() => !state.id || !props.transaction?.categoryID)
const NO_CATEGORY = '__no_category__'
const categoryOptions = computed(() => [
  { id: NO_CATEGORY, title: t('noCategory') },
  ...props.categories
    .filter(category => category.income === state.income)
    .map(category => ({ id: category.id, title: categoryLabel(category.title, category.defaultKey) }))
])

function selectedCategoryId(value?: string) {
  return !value || value === NO_CATEGORY ? undefined : value
}

watch(
  () => props.transaction,
  (t) => {
    if (!t) {
      resetState()
      return
    }
    const matchingCategory = props.categories.find(category => category.title === t.category)
    Object.assign(state, {
      id: t.id,
      title: t.title,
      value: t.value,
      categoryID: t.categoryID ?? matchingCategory?.id,
      income: t.income,
      pending: false,
      executionDate: t.executionDate ? parseLocalDate(t.executionDate) : undefined
    })
    open.value = true
  }
)

watch(() => state.income, (income) => {
  const selected = props.categories.find(category => category.id === state.categoryID)
  if (selected && selected.income !== income) state.categoryID = undefined
})

async function onSubmit(event: FormSubmitEvent<Schema>) {
  const { $financesService } = useNuxtApp()
  const executionDate = event.data.executionDate
    ? toExecutionDate(event.data.executionDate)
    : toExecutionDate(new Date())
  if (event.data.id) {
    await $financesService.editTransaction(event.data.id, {
      title: event.data.title,
      value: event.data.value,
      executionDate,
      ...(canEditCategory.value ? { categoryID: selectedCategoryId(event.data.categoryID), income: event.data.income } : {})
    })
    toast.add({
      title: 'Success',
      description: `Transaction ${event.data.title} updated`,
      color: 'success'
    })
  } else {
    if (!activeAccount.value) return
    await $financesService.createTransaction({
      income: event.data.income,
      value: event.data.value,
      categoryID: selectedCategoryId(event.data.categoryID),
      title: event.data.title,
      executionDate,
      accountId: activeAccount.value.id,
      status: event.data.pending ? 'PENDING' : 'OK'
    })
    toast.add({
      title: 'Success',
      description: `New transaction ${event.data.title} added`,
      color: 'success'
    })
  }

  open.value = false
  await props.refresh()
}

function resetState() {
  Object.assign(state, {
    id: undefined,
    title: undefined,
    value: undefined,
    categoryID: undefined,
    income: false,
    pending: false,
    executionDate: undefined
  })
}

function openModal() {
  if (props.transaction) {
    const matchingCategory = props.categories.find(category => category.title === props.transaction?.category)
    Object.assign(state, {
      id: props.transaction.id,
      title: props.transaction.title,
      value: props.transaction.value,
      categoryID: props.transaction.categoryID ?? matchingCategory?.id,
      income: props.transaction.income,
      pending: false,
      executionDate: props.transaction.executionDate ? parseLocalDate(props.transaction.executionDate) : undefined
    })
  } else {
    resetState()
  }
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
    :title="props.transaction ? $t('editTransaction') : $t('newTransaction')"
    :description="props.transaction ? $t('editTransactionDesc') : $t('newTransactionDesc')"
  >
    <template #body>
      <UForm
        :schema="schema"
        :state="state"
        class="space-y-4"
        @submit="onSubmit"
      >
        <UFormField
          :label="$t('title')"
          :placeholder="$t('enterTitle')"
          name="title"
        >
          <UInput
            v-model="state.title"
            class="w-full"
          />
        </UFormField>
        <UFormField
          :label="$t('value')"
          :placeholder="$t('enterValue')"
          name="value"
        >
          <UInput
            v-model="state.value"
            class="w-full"
            type="number"
          />
        </UFormField>

        <UFormField
          v-if="canEditCategory"
          :label="$t('type')"
          name="income"
        >
          <div class="flex items-center gap-3">
            <USwitch v-model="state.income" />
            <span class="text-sm text-muted">{{ state.income ? $t('income') : $t('expanse') }}</span>
          </div>
        </UFormField>

        <UFormField
          v-if="canEditCategory"
          :label="$t('category')"
          name="category"
        >
          <UInputMenu
            v-model="state.categoryID"
            value-key="id"
            label-key="title"
            :items="categoryOptions"
            class="w-full"
          />
        </UFormField>
        <UFormField
          v-else
          :label="$t('category')"
        >
          <UInput
            :model-value="categoryLabel(props.transaction?.category, props.transaction?.categoryKey)"
            disabled
            class="w-full"
          />
        </UFormField>

        <UFormField
          :label="$t('executionDate')"
          name="executionDate"
        >
          <UInputDate v-model="state.executionDate" :locale="localeTag" />
        </UFormField>

        <UFormField
          v-if="!state.id"
          :label="$t('pendingTransaction')"
          name="pending"
          :help="$t('pendingTransactionHelp')"
        >
          <div class="flex items-center gap-3">
            <USwitch v-model="state.pending" />
            <span class="text-sm text-muted">{{ state.pending ? $t('transactionStatus.pending') : $t('transactionStatus.ok') }}</span>
          </div>
        </UFormField>

        <div class="flex justify-end gap-2">
          <UButton
            :label="$t('cancel')"
            color="neutral"
            variant="subtle"
            @click="closeModal"
          />
          <UButton
            :label="$t('save')"
            color="primary"
            variant="solid"
            type="submit"
          />
        </div>
      </UForm>
    </template>
  </UModal>
</template>
