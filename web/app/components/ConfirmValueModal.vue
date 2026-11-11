<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'

interface ConfirmValueOptions {
  title: string
  description?: string
  help?: string
  submitLabel?: string
  onConfirm: (value: number) => Promise<void> | void
}

const { t } = useI18n()
const toast = useToast()
const open = ref(false)
const loading = ref(false)
const options = ref<ConfirmValueOptions | null>(null)
const state = reactive<{ value?: number }>({ value: undefined })

const schema = z.object({
  value: z.coerce.number().positive(t('confirmValueInvalid'))
})

type Schema = z.output<typeof schema>

function openModal(defaultValue: number, config: ConfirmValueOptions) {
  options.value = config
  state.value = defaultValue
  open.value = true
}

function closeModal() {
  if (loading.value) return
  open.value = false
}

async function onSubmit(event: FormSubmitEvent<Schema>) {
  if (!options.value) return
  loading.value = true
  try {
    await options.value.onConfirm(event.data.value)
    open.value = false
  } catch {
    toast.add({ title: t('confirmValueFailed'), color: 'error' })
  } finally {
    loading.value = false
  }
}

function reset() {
  options.value = null
  state.value = undefined
}

defineExpose({ openModal, closeModal })
</script>

<template>
  <UModal
    v-model:open="open"
    :title="options?.title || $t('confirm')"
    :description="options?.description"
    @update:open="value => { if (!value) reset() }"
  >
    <template #body>
      <UForm
        :schema="schema"
        :state="state"
        class="space-y-4"
        @submit="onSubmit"
      >
        <UFormField
          :label="$t('value')"
          name="value"
          required
        >
          <UInput
            v-model="state.value"
            type="number"
            min="0.01"
            step="0.01"
            class="w-full"
          />
        </UFormField>

        <p class="text-xs text-muted">
          {{ options?.help || $t('confirmValueHelp') }}
        </p>

        <div class="flex justify-end gap-2">
          <UButton
            :label="$t('cancel')"
            color="neutral"
            variant="subtle"
            :disabled="loading"
            @click="closeModal"
          />
          <UButton
            :label="options?.submitLabel || $t('confirm')"
            color="primary"
            type="submit"
            :loading="loading"
          />
        </div>
      </UForm>
    </template>
  </UModal>
</template>
