<script setup lang="ts">
interface DeleteConfirmOptions {
  title: string
  description?: string
  submitLabel?: string
  onConfirm: () => Promise<void> | void
}

const props = defineProps({
  loading: { type: Boolean, default: false }
})

const options = ref<DeleteConfirmOptions | null>(null)
const open = ref(false)
const localLoading = ref(false)

async function confirm() {
  if (!options.value) return
  localLoading.value = true
  try {
    await options.value.onConfirm()
    open.value = false
    options.value = null
  } finally {
    localLoading.value = false
  }
}

function openModal(config: DeleteConfirmOptions) {
  options.value = config
  open.value = true
}

function closeModal() {
  open.value = false
  options.value = null
}

defineExpose({ openModal, closeModal })
</script>

<template>
  <UModal
    :open="open"
    :title="options?.title || $t('confirm')"
    :description="options?.description"
    @update:open="value => { if (!value) closeModal() }"
  >
    <template #body>
      <div class="flex justify-end gap-2">
        <UButton
          :label="$t('cancel')"
          color="neutral"
          variant="subtle"
          :disabled="localLoading || props.loading"
          @click="closeModal"
        />
        <UButton
          :label="options?.submitLabel || $t('delete')"
          color="error"
          variant="solid"
          :loading="localLoading || props.loading"
          @click="confirm"
        />
      </div>
    </template>
  </UModal>
</template>
