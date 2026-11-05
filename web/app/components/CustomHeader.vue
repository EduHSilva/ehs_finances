<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'
import type { CreateAccountRequest } from '~/services/financesService'

const { locale, setLocale } = useI18n()
const { accountOptions, activeAccount, setActiveAccount, loadAccounts } = useFinancialAccounts()
const financePreferences = useFinancePreferences()
const { $financesService } = useNuxtApp()
const toast = useToast()
const createAccountOpen = ref(false)
const accountSchema = z.object({
  name: z.string().trim().min(2).max(120),
  type: z.enum(['PF', 'PJ']),
  openingBalance: z.coerce.number()
})
type AccountForm = z.output<typeof accountSchema>
const accountState = reactive<Partial<AccountForm>>({ name: '', type: 'PF', openingBalance: 0 })

onMounted(async () => {
  const results = await Promise.allSettled([loadAccounts(), financePreferences.load()])
  for (const result of results) {
    if (result.status === 'rejected') console.error('Failed to load finance settings', result.reason)
  }
})

async function createAccount(event: FormSubmitEvent<AccountForm>) {
  try {
    const response = await $financesService.createAccount(event.data as CreateAccountRequest)
    await loadAccounts(true)
    setActiveAccount(response.data.id)
    createAccountOpen.value = false
    Object.assign(accountState, { name: '', type: 'PF', openingBalance: 0 })
    toast.add({ title: $t('accountCreated'), color: 'success', icon: 'i-lucide-check' })
  } catch {
    toast.add({ title: $t('accountCreateFailed'), color: 'error' })
  }
}

const localeOptions = [
  { value: 'pt', label: '🇧🇷 Português' },
  { value: 'en', label: '🇺🇸 English' }
]

function updateLocale(value: string) {
  setLocale(value as 'pt' | 'en')
}
</script>

<template>
  <UDashboardNavbar
    :title="$t('manageFinances')"
    :ui="{ right: 'gap-3' }"
  >
    <template #leading>
      <UDashboardSidebarCollapse />
    </template>

    <template #right>
      <slot />

      <UBadge
        v-if="activeAccount"
        color="primary"
        variant="subtle"
        class="md:hidden"
      >
        {{ activeAccount?.type }}
      </UBadge>
      <UButton icon="i-lucide-plus" color="neutral" variant="ghost" class="md:hidden" :aria-label="$t('newAccount')" @click="createAccountOpen = true" />

      <div class="hidden items-center gap-2 md:flex">
        <UBadge
          v-if="activeAccount"
          color="primary"
          variant="subtle"
        >
          {{ activeAccount?.type }}
        </UBadge>
        <USelect
          v-if="activeAccount"
          :model-value="activeAccount.id"
          :items="accountOptions"
          size="sm"
          class="w-44"
          @update:model-value="setActiveAccount($event)"
        />
        <UButton icon="i-lucide-plus" color="neutral" variant="ghost" :aria-label="$t('newAccount')" @click="createAccountOpen = true" />
      </div>

      <UColorModeSwitch />
      <USelect
        v-model="locale"
        :items="localeOptions"
        option-attribute="label"
        value-attribute="value"
        size="sm"
        @update:model-value="updateLocale"
      />
    </template>
  </UDashboardNavbar>

  <UModal v-model:open="createAccountOpen" :title="$t('newAccount')">
    <template #body>
      <UForm :schema="accountSchema" :state="accountState" class="space-y-4" @submit="createAccount">
        <UFormField :label="$t('accountName')" name="name" required>
          <UInput v-model="accountState.name" class="w-full" />
        </UFormField>
        <UFormField :label="$t('accountType')" name="type" required>
          <USelect v-model="accountState.type" :items="[{ label: 'PF', value: 'PF' }, { label: 'PJ', value: 'PJ' }]" class="w-full" />
        </UFormField>
        <UFormField :label="$t('openingBalance')" name="openingBalance" required>
          <UInput v-model="accountState.openingBalance" type="number" step="0.01" class="w-full" />
        </UFormField>
        <div class="flex justify-end gap-2">
          <UButton color="neutral" variant="subtle" :label="$t('cancel')" @click="createAccountOpen = false" />
          <UButton type="submit" :label="$t('save')" />
        </div>
      </UForm>
    </template>
  </UModal>
</template>

<style scoped>

</style>
