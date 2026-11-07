<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'
import type { Category, Recurrence } from '~/types'

const props = defineProps({
  refresh: { type: Function, required: true },
  categories: { type: Array as PropType<Category[]>, required: true },
  recurrency: { type: Object as () => Recurrence | null, default: null }
})
const { activeAccount } = useFinancialAccounts()
const categoryLabel = useCategoryLabel()
const toast = useToast()
const open = ref(false)

const schema = z.object({
  id: z.string().optional(),
  title: z.string().min(2, 'Too short'),
  day: z.coerce.number().min(1).max(31),
  value: z.coerce.number(),
  income: z.boolean(),
  categoryID: z.string().optional(),
  type: z.enum(['MONTH', 'INSTALLMENT']),
  installments: z.coerce.number().optional(),
  payed: z.coerce.number().optional()
})
  .superRefine((data, ctx) => {
    if (data.type === 'INSTALLMENT') {
      if (data.installments == null) {
        ctx.addIssue({
          code: 'custom',
          path: ['installments'],
          message: 'Required'
        })
      }

      if (data.payed == null) {
        ctx.addIssue({
          code: 'custom',
          path: ['payed'],
          message: 'Required'
        })
      }
    } else {
      data.installments = undefined
      data.payed = undefined
    }
  })

type Schema = z.output<typeof schema>

const state = reactive<Partial<Schema>>({
  id: undefined,
  title: undefined,
  day: undefined,
  value: undefined,
  income: false,
  categoryID: undefined,
  type: 'MONTH',
  installments: undefined,
  payed: undefined
})

const canEditCategory = computed(() => !state.id || !props.recurrency?.categoryID)
const categoryOptions = computed(() => props.categories
  .filter(category => category.income === state.income)
  .map(category => ({ ...category, title: categoryLabel(category.title, category.defaultKey) })))

watch(
  () => props.recurrency,
  (c) => {
    if (!c) {
      resetState()
      return
    }
    applyRecurrence(c)
    open.value = true
  }
)

watch(() => state.income, (income) => {
  const selected = props.categories.find(category => category.id === state.categoryID)
  if (selected && selected.income !== income) state.categoryID = undefined
})

const showInstallments = computed(() => state.type === 'INSTALLMENT')

function applyRecurrence(c: Recurrence) {
  Object.assign(state, {
    id: c.id,
    title: c.title,
    day: c.day,
    value: c.value,
    income: c.income,
    categoryID: c.categoryID,
    type: c.type,
    installments: c.installments ?? undefined,
    payed: c.payed ?? undefined
  })
}

async function onSubmit(event: FormSubmitEvent<Schema>) {
  const { $financesService } = useNuxtApp()
  if (canEditCategory.value && !event.data.categoryID) {
    toast.add({ title: $t('categoryRequired'), color: 'error' })
    return
  }

  const payload: Recurrence = {
    ...event.data,
    accountId: activeAccount.value?.id,
    status: props.recurrency?.status
  }

  if (event.data.id) {
    if (!canEditCategory.value) {
      payload.categoryID = undefined
      payload.income = undefined as unknown as boolean
    }
    await $financesService.editRecurrence(event.data.id, payload)
    toast.add({
      title: 'Success',
      description: `Recurrence ${event.data.title} updated`,
      color: 'success'
    })
  } else {
    if (!activeAccount.value) return
    payload.status = 'ACTIVE'
    await $financesService.createRecurrence(payload)
    toast.add({
      title: 'Success',
      description: `New recurrence ${event.data.title} added`,
      color: 'success'
    })
  }

  closeModal()
  await props.refresh()
}

function resetState() {
  Object.assign(state, {
    id: undefined,
    title: undefined,
    day: undefined,
    value: undefined,
    income: false,
    categoryID: undefined,
    type: 'MONTH',
    installments: undefined,
    payed: undefined
  })
}

function openModal() {
  if (props.recurrency) applyRecurrence(props.recurrency)
  else resetState()
  open.value = true
}

function closeModal() {
  open.value = false
  resetState()
}

const recurrenceTypes = [
  { id: 'MONTH', label: $t('monthly') },
  { id: 'INSTALLMENT', label: $t('installments') }
]

defineExpose({ openModal, closeModal })
</script>

<template>
  <UModal
    v-model:open="open"
    :title="state.id ? $t('editRecurrence') : $t('newRecurrence')"
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
          name="title"
        >
          <UInput
            v-model="state.title"
            class="w-full"
          />
        </UFormField>

        <div class="flex gap-2 items-center">
          <UFormField
            :label="$t('day')"
            name="day"
          >
            <UInput
              v-model="state.day"
              type="number"
              class="w-full"
            />
          </UFormField>

          <UFormField
            :label="$t('value')"
            name="value"
          >
            <UInput
              v-model="state.value"
              type="number"
              class="w-full"
            />
          </UFormField>
        </div>

        <UFormField
          v-if="!state.id"
          :label="$t('type')"
          name="type"
        >
          <UInputMenu
            v-model="state.type"
            value-key="id"
            :items="recurrenceTypes"
            class="w-full"
          />
        </UFormField>

        <UFormField
          v-if="canEditCategory"
          :label="$t('movementType')"
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
          name="categoryID"
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
            :model-value="categoryLabel(props.recurrency?.category, props.recurrency?.categoryKey)"
            disabled
            class="w-full"
          />
        </UFormField>

        <UFormField
          v-if="showInstallments"
          :label="$t('installments')"
          name="installments"
        >
          <UInput
            v-model="state.installments"
            type="number"
            class="w-full"
          />
        </UFormField>

        <UFormField
          v-if="showInstallments"
          :label="$t('payed')"
          name="payed"
        >
          <UInput
            v-model="state.payed"
            type="number"
            class="w-full"
          />
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
