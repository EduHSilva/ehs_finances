<script setup lang="ts">
import type { NavigationMenuItem } from '@nuxt/ui'

const route = useRoute()
const toast = useToast()

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

onMounted(async () => {
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
</template>
