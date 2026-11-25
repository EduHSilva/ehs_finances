<script setup lang="ts">
import type { FinancePreferences } from '~/types'

const { $financesService } = useNuxtApp()
const { t } = useI18n()
const { preferences, loading, saving, load, save } = useFinancePreferences()
const { activeAccount, loadAccounts } = useFinancialAccounts()
const showBusinessAccount = ref(true)
const accountName = ref('')
const savingAccount = ref(false)
const toast = useToast()

const isOwner = computed(() => (activeAccount.value?.role ?? 'OWNER') === 'OWNER')

watch(activeAccount, (account) => {
  accountName.value = account?.name || ''
}, { immediate: true })

onMounted(async () => {
  try {
    await Promise.all([load(), loadAccounts()])
    showBusinessAccount.value = preferences.value.showBusinessAccount
  } catch {
    toast.add({ title: t('settingsLoadFailed'), color: 'error' })
  }
})

async function onSaveAccountName() {
  if (!activeAccount.value || !isOwner.value || savingAccount.value) return
  const name = accountName.value.trim()
  if (name.length < 2) {
    toast.add({ title: t('accountNameInvalid'), color: 'error' })
    return
  }
  savingAccount.value = true
  try {
    await $financesService.updateAccount(activeAccount.value.id, { name })
    await loadAccounts(true)
    toast.add({ title: t('accountUpdated'), color: 'success', icon: 'i-lucide-check' })
  } catch {
    toast.add({ title: t('accountUpdateFailed'), color: 'error' })
  } finally {
    savingAccount.value = false
  }
}

async function onSave() {
  const updated: FinancePreferences = {
    ...preferences.value,
    showBusinessAccount: showBusinessAccount.value
  }
  try {
    await save(updated)
    toast.add({ title: t('preferencesSaved'), color: 'success', icon: 'i-lucide-check' })
  } catch {
    toast.add({ title: t('preferencesSaveFailed'), color: 'error' })
  }
}
</script>

<template>
  <div class="space-y-4">
    <UPageCard
      :title="t('accountSettings')"
      :description="t('accountSettingsDescription')"
      variant="naked"
      orientation="horizontal"
      class="mb-4"
    >
      <UButton
        :label="t('save')"
        color="primary"
        class="w-fit lg:ms-auto"
        :loading="savingAccount"
        :disabled="!isOwner || savingAccount"
        @click="onSaveAccountName"
      />
    </UPageCard>

    <UPageCard variant="subtle">
      <UFormField
        :label="t('accountName')"
        name="accountName"
      >
        <UInput
          v-model="accountName"
          :disabled="!isOwner || savingAccount"
          maxlength="120"
          class="w-full"
        />
      </UFormField>
      <p
        v-if="!isOwner"
        class="mt-2 text-sm text-muted"
      >
        {{ t('accountOwnerOnly') }}
      </p>
    </UPageCard>

    <UPageCard
      :title="t('preferences')"
      :description="t('financePreferencesDescription')"
      variant="naked"
      orientation="horizontal"
      class="mb-4"
    >
      <UButton
        :label="t('save')"
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
          <div class="font-medium text-highlighted">{{ t('showBusinessAccount') }}</div>
          <p id="show-business-account-description" class="text-sm text-muted">
            {{ t('showBusinessAccountDescription') }}
          </p>
        </div>
      </div>
      <div v-if="loading" class="mt-4">
        <USkeleton class="h-4 w-48" />
      </div>
    </UPageCard>
  </div>
</template>
