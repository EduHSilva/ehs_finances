<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'
import type { Category } from '~/types'

const props = defineProps({
  refresh: { type: Function, required: true },
  category: { type: Object as () => Category | null, default: null }
})

const open = ref(false)

const schema = z.object({
  id: z.string().optional(),
  title: z.string().min(2, 'Too short'),
  goal: z.coerce.number().min(0).max(100),
  classification: z.string().min(2, 'Too short'),
  color: z.string().min(2, 'Too short'),
  income: z.boolean().default(false)
})

type Schema = z.output<typeof schema>

const state = reactive<Partial<Schema>>({
  title: undefined,
  id: undefined,
  goal: 0,
  classification: 'ESSENTIAL',
  color: undefined,
  income: false
})

watch(
  () => props.category,
  (c) => {
    if (!c) return
    Object.assign(state, {
      id: c.id,
      title: c.title,
      goal: c.goal,
      classification: c.classification || 'ESSENTIAL',
      color: c.color,
      income: c.income
    })
    open.value = true
  },
  { immediate: true }
)

watch(() => state.income, (income) => {
  if (income) state.goal = 0
})

const chip = computed(() => ({ backgroundColor: state.color }))

const toast = useToast()

async function onSubmit(event: FormSubmitEvent<Schema>) {
  const { $financesService } = useNuxtApp()
  const payload = {
    ...event.data,
    goal: event.data.income ? 0 : event.data.goal,
    classification: event.data.income ? 'ESSENTIAL' : event.data.classification
  } as Category

  if (event.data.id) {
    await $financesService.editCategory(event.data.id, payload)
    toast.add({
      title: $t('success'),
      description: $t('update'),
      color: 'success'
    })
  } else {
    await $financesService.createCategory(payload)
    toast.add({
      title: 'Success',
      description: `New category ${event.data.title} added`,
      color: 'success'
    })
  }

  open.value = false
  await props.refresh()
}

const items = ref([
  {
    label: $t('essential'),
    id: 'ESSENTIAL'
  },
  {
    label: $t('recreation'),
    id: 'RECREATION'
  },
  {
    label: $t('investment'),
    id: 'INVESTMENT'
  }]
)

function resetState() {
  Object.assign(state, {
    id: undefined,
    title: undefined,
    goal: 0,
    classification: 'ESSENTIAL',
    color: undefined,
    income: false
  })
}

function openModal() {
  if (!props.category) resetState()
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
    :title="props.category ? $t('editCategory') : $t('newCategory')"
    :description="props.category ? $t('editCategoryDesc') : $t('newCategoryDesc')"
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
          :label="$t('categoryDirection')"
          :description="$t('categoryDirectionHelp')"
          name="income"
        >
          <div class="flex items-center gap-3">
            <USwitch v-model="state.income" />
            <span class="text-sm text-muted">
              {{ state.income ? $t('income') : $t('expanse') }}
            </span>
          </div>
        </UFormField>

        <template v-if="!state.income">
          <UFormField
            :label="$t('goal')"
            :description="$t('categoryGoalHelp')"
            :placeholder="$t('enterGoal')"
            name="goal"
          >
            <UInput
              v-model="state.goal"
              class="w-full"
              type="number"
              min="0"
              max="100"
            />
          </UFormField>
          <UFormField
            :label="$t('classification')"
            :placeholder="$t('enterClassification')"
            name="classification"
          >
            <UInputMenu
              v-model="state.classification"
              value-key="id"
              :items="items"
              class="w-full"
            />
          </UFormField>
        </template>

        <UPopover>
          <UButton
            :label="$t('color')"
            color="neutral"
            variant="outline"
          >
            <template #leading>
              <span
                :style="chip"
                class="size-3 rounded-full"
              />
            </template>
          </UButton>

          <template #content>
            <UColorPicker
              v-model="state.color"
              class="p-2"
            />
          </template>
        </UPopover>
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
