<script setup lang="ts">
import type { DropdownMenuItem } from '@nuxt/ui'
import { useUser } from '~/plugins/userService'

defineProps<{
  collapsed?: boolean
}>()

const colorMode = useColorMode()
const user = useUser()
const username = computed(() => user.value?.username || '')

const items = computed<DropdownMenuItem[][]>(() => [[{
  type: 'label',
  label: username.value
}], [{
  label: $t('appearance'),
  icon: 'i-lucide-sun-moon',
  children: [{
    label: $t('light'),
    icon: 'i-lucide-sun',
    type: 'checkbox',
    checked: colorMode.value === 'light',
    onSelect(event: Event) {
      event.preventDefault()
      colorMode.preference = 'light'
    }
  }, {
    label: $t('dark'),
    icon: 'i-lucide-moon',
    type: 'checkbox',
    checked: colorMode.value === 'dark',
    onSelect(event: Event) {
      event.preventDefault()
      colorMode.preference = 'dark'
    }
  }, {
    label: $t('system'),
    icon: 'i-lucide-monitor',
    type: 'checkbox',
    checked: colorMode.value === 'system',
    onSelect(event: Event) {
      event.preventDefault()
      colorMode.preference = 'system'
    }
  }]
}]])
</script>

<template>
  <UDropdownMenu
    :items="items"
    :content="{ align: 'center', collisionPadding: 12 }"
    :ui="{ content: collapsed ? 'w-52' : 'w-(--reka-dropdown-menu-trigger-width)' }"
  >
    <UButton
      v-bind="user"
      :label="collapsed ? undefined : username"
      :trailing-icon="collapsed ? undefined : 'i-lucide-chevrons-up-down'"
      color="neutral"
      variant="ghost"
      block
      :square="collapsed"
      class="data-[state=open]:bg-elevated"
      :ui="{ trailingIcon: 'text-dimmed' }"
    />
  </UDropdownMenu>
</template>
