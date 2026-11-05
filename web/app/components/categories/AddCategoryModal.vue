<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'
import type { Category } from '~/types'

const props = defineProps({
  refresh: { type: Function, required: true },
  category: { type: Object as () => Category | null, default: null }
})

watch(
  () => props.category,
  (c) => {
    if (!c) return
    state.id = c.id
    state.title = c.title
    state.goal = c.goal
    state.classification = c.classification
    state.color = c.color
    open.value = true
  },
  { immediate: true }
)

const chip = computed(() => ({ backgroundColor: state.color }))

const schema = z.object({
  id: z.string().optional(),
  title: z.string().min(2, 'Too short'),
  goal: z.coerce.number(),
  classification: z.string().min(2, 'Too short'),
  color: z.string().min(2, 'Too short')
})
const open = ref(false)

type Schema = z.output<typeof schema>

const state = reactive<Partial<Schema>>({
  title: undefined,
  id: undefined,
  goal: undefined,
  classification: undefined,
  color: undefined
})

const toast = useToast()

async function onSubmit(event: FormSubmitEvent<Schema>) {
  const { $financesService } = useNuxtApp()

  if (event.data.id) {
    await $financesService.editCategory(event.data.id, event.data as Category)
    toast.add({
      title: $t('success'),
      description: $t('update'),
      color: 'success'
    })
  } else {
    await $financesService.createCategory(event.data as Category)
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
  }]
)

function openModal() {
  open.value = true
}

function closeModal() {
  open.value = false
  Object.assign(state, {
    id: undefined,
    title: undefined,
    goal: undefined,
    classification: undefined,
    color: undefined
  })
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
          :label="$t('goal')"
          :placeholder="$t('enterGoal')"
          name="goal"
        >
          <UInput
            v-model="state.goal"
            class="w-full"
            type="number"
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
