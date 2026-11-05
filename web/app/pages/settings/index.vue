<script setup lang="ts">
import type { FinancePreferences } from '~/types'

const { preferences, loading, saving, load, save } = useFinancePreferences()
const showBusinessAccount = ref(true)
const toast = useToast()

onMounted(async () => {
  try {
    await load()
    showBusinessAccount.value = preferences.value.showBusinessAccount
  } catch {
    toast.add({ title: 'Não foi possível carregar as preferências', color: 'error' })
  }
})

async function onSave() {
  const updated: FinancePreferences = {
    ...preferences.value,
    showBusinessAccount: showBusinessAccount.value
  }
  try {
    await save(updated)
    toast.add({ title: 'Preferências salvas', color: 'success', icon: 'i-lucide-check' })
  } catch {
    toast.add({ title: 'Não foi possível salvar as preferências', color: 'error' })
  }
}
</script>

<template>
  <div class="space-y-4">
    <UPageCard
      :title="$t('preferences')"
      :description="$t('financePreferencesDescription')"
      variant="naked"
      orientation="horizontal"
      class="mb-4"
    >
      <UButton
        :label="$t('save')"
        color="primary"
        class="w-fit lg:ms-auto"
        :loading="saving"
        :disabled="loading || saving"
        @click="onSave"
      />
    </UPageCard>

    <UPageCard variant="subtle">
      <div class="flex items-start gap-3">
        <UCheckbox
          v-model="showBusinessAccount"
          :disabled="loading || saving"
          aria-describedby="show-business-account-description"
        />
        <div class="space-y-1">
          <div class="font-medium text-highlighted">{{ $t('showBusinessAccount') }}</div>
          <p id="show-business-account-description" class="text-sm text-muted">
            {{ $t('showBusinessAccountDescription') }}
          </p>
        </div>
      </div>
      <div v-if="loading" class="mt-4">
        <USkeleton class="h-4 w-48" />
      </div>
    </UPageCard>
  </div>
</template>
