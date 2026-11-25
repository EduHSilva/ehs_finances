<script setup lang="ts">
import type { NavigationMenuItem } from '@nuxt/ui'
import { useUser } from '~/plugins/userService'

const route = useRoute()
const toast = useToast()
const { t } = useI18n()
const user = useUser()
const { $financesService } = useNuxtApp()
const { accounts, loadAccounts } = useFinancialAccounts()

const open = ref(false)

const links = [[{
  label: $t('dashboard'),
  icon: 'i-lucide-house',
  to: '/dashboard',
  onSelect: () => {
    open.value = false
  }
}, {
  label: $t('transactions'),
  icon: 'i-lucide-dollar-sign',
  to: '/transactions',
  onSelect: () => {
    open.value = false
  }
}, {
  label: $t('categories'),
  icon: 'i-lucide-layers-2',
  to: '/categories',
  onSelect: () => {
    open.value = false
  }
}, {
  label: $t('navGoals'),
  icon: 'i-lucide-target',
  to: '/goals',
  onSelect: () => {
    open.value = false
  }
}, {
  label: $t('investments'),
  icon: 'i-lucide-trending-up',
  to: '/investments',
  onSelect: () => {
    open.value = false
  }
}, {
  label: $t('recurrences'),
  icon: 'i-lucide-calendar-sync',
  to: '/recurrences',
  onSelect: () => {
    open.value = false
  }
}, {
  label: $t('cards'),
  icon: 'i-lucide-credit-card',
  to: '/cards',
  onSelect: () => {
    open.value = false
  }
},
{
  label: $t('settings'),
  to: '/settings',
  icon: 'i-lucide-settings',
  defaultOpen: true,
  type: 'trigger',
  children: [{
    label: $t('general'),
    to: '/settings',
    exact: true,
    onSelect: () => {
      open.value = false
    }
  }, {
    label: $t('members'),
    to: '/settings/members',
    onSelect: () => {
      open.value = false
    }
  }, {
    label: $t('notifications'),
    to: '/settings/notifications',
    onSelect: () => {
      open.value = false
    }
  }, {
    label: $t('security'),
    to: '/settings/security',
    onSelect: () => {
      open.value = false
    }
  }]
}], [{
  label: $t('sourceCode'),
  icon: 'i-lucide-github',
  to: 'https://github.com/EduHSilva/finances',
  target: '_blank'
}]] satisfies NavigationMenuItem[][]

const groups = computed(() => [{
  id: 'links',
  label: 'Go to',
  items: links.flat()
}, {
  id: 'code',
  label: 'Code',
  items: [{
    id: 'source',
    label: 'View page source',
    icon: 'i-simple-icons-github',
    to: `https://github.com/nuxt-ui-templates/dashboard/blob/main/app/pages${route.path === '/' ? '/index' : route.path}.vue`,
    target: '_blank'
  }]
}])

const setupKey = computed(() => `finances-account-setup:${user.value?.id || 'anonymous'}`)
const setupOpen = ref(false)
const setupName = ref('')
const setupBalance = ref<number | string>(0)
const savingSetup = ref(false)

/*
 * A conta pessoal padrão é criada automaticamente pelo backend sem saldo inicial.
 * Enquanto ela estiver intocada (nome padrão, sem saldo e sem movimentação), o
 * primeiro login oferece nome + saldo inicial.
 */
const setupAccount = computed(() => {
  if (!user.value || accounts.value.length !== 1) return null
  const account = accounts.value[0]
  if (!account) return null
  if (account.id !== `PF-${user.value.id}`) return null
  if (account.name !== 'Conta pessoal' || Number(account.openingBalance) !== 0) return null
  if (Number(account.balance) !== 0) return null
  return account
})

watch(setupAccount, (account) => {
  if (!account || setupOpen.value) return
  if (typeof localStorage !== 'undefined' && localStorage.getItem(setupKey.value)) return
  setupName.value = user.value?.username || account.name
  setupBalance.value = 0
  setupOpen.value = true
}, { immediate: true })

function dismissSetup() {
  if (typeof localStorage !== 'undefined') localStorage.setItem(setupKey.value, '1')
  setupOpen.value = false
}

async function saveSetup() {
  const account = setupAccount.value
  if (!account || savingSetup.value) return
  const name = setupName.value.trim()
  const balance = Number(setupBalance.value)
  if (name.length < 2) {
    toast.add({ title: t('accountNameInvalid'), color: 'error' })
    return
  }
  if (!Number.isFinite(balance) || balance < 0) {
    toast.add({ title: t('openingBalanceInvalid'), color: 'error' })
    return
  }
  savingSetup.value = true
  try {
    await $financesService.updateAccount(account.id, { name, openingBalance: balance })
    await loadAccounts(true)
    dismissSetup()
    toast.add({ title: t('accountUpdated'), color: 'success', icon: 'i-lucide-check' })
  } catch {
    toast.add({ title: t('accountUpdateFailed'), color: 'error' })
  } finally {
    savingSetup.value = false
  }
}

onMounted(async () => {
  loadAccounts().catch(() => {
    // O erro de carregamento já aparece nas telas; aqui só tentamos alimentar o setup.
  })

  const cookie = useCookie('cookie-consent')
  if (cookie.value === 'accepted') {
    return
  }

  toast.add({
    title: 'We use first-party cookies to enhance your experience on our website.',
    duration: 0,
    close: false,
    actions: [{
      label: 'Accept',
      color: 'neutral',
      variant: 'outline',
      onClick: () => {
        cookie.value = 'accepted'
      }
    }, {
      label: 'Opt out',
      color: 'neutral',
      variant: 'ghost'
    }]
  })
})
</script>

<template>
  <UDashboardGroup unit="rem">
    <UDashboardSidebar
      id="default"
      v-model:open="open"
      collapsible
      resizable
      class="app-sidebar"
      :ui="{ root: 'app-sidebar', footer: 'lg:border-t' }"
    >
      <template #header>
        <BrandLogo />
      </template>

      <template #default="{ collapsed }">
        <UDashboardSearchButton
          :collapsed="collapsed"
          class="app-sidebar-search bg-transparent ring-default"
        />

        <UNavigationMenu
          :collapsed="collapsed"
          :items="links[0]"
          orientation="vertical"
          tooltip
          popover
        />

        <UNavigationMenu
          :collapsed="collapsed"
          :items="links[1]"
          orientation="vertical"
          tooltip
          class="mt-auto"
        />
      </template>

      <template #footer="{ collapsed }">
        <UserMenu :collapsed="collapsed" />
        <p v-if="!collapsed" class="px-2 pt-2 pb-1 text-[11px] text-(--brand-sidebar-muted)">Desenvolvido por EHS Solutions</p>
      </template>
    </UDashboardSidebar>

    <UDashboardSearch :groups="groups" />

    <slot />
  </UDashboardGroup>

  <UModal
    v-model:open="setupOpen"
    :title="t('accountSetupTitle')"
    :description="t('accountSetupDescription')"
    :dismissible="!savingSetup"
    @update:open="value => { if (!value) dismissSetup() }"
  >
    <template #body>
      <form
        class="space-y-4"
        @submit.prevent="saveSetup"
      >
        <UFormField
          :label="t('accountName')"
          name="setupName"
          required
        >
          <UInput
            v-model="setupName"
            maxlength="120"
            class="w-full"
            autofocus
          />
        </UFormField>
        <UFormField
          :label="t('openingBalance')"
          name="setupBalance"
          :help="t('accountSetupBalanceHelp')"
        >
          <UInput
            v-model="setupBalance"
            type="number"
            min="0"
            step="0.01"
            class="w-full"
          />
        </UFormField>

        <div class="flex justify-end gap-2">
          <UButton
            :label="t('later')"
            color="neutral"
            variant="subtle"
            :disabled="savingSetup"
            @click="dismissSetup"
          />
          <UButton
            :label="t('save')"
            type="submit"
            :loading="savingSetup"
          />
        </div>
      </form>
    </template>
  </UModal>
</template>
