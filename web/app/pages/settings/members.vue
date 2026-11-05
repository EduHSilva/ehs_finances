<script setup lang="ts">
import type { Member } from '~/types'

const members = useState<Member[]>('settings-members', () => [{
  name: 'Eduardo Silva',
  username: 'eduardo',
  role: 'owner',
  avatar: {
    src: 'https://i.pravatar.cc/80?img=12',
    alt: 'Eduardo Silva'
  }
}, {
  name: 'Financeiro',
  username: 'financeiro',
  role: 'member',
  avatar: {
    src: 'https://i.pravatar.cc/80?img=32',
    alt: 'Financeiro'
  }
}])

const q = ref('')

const filteredMembers = computed(() => {
  return members.value.filter((member) => {
    return member.name.search(new RegExp(q.value, 'i')) !== -1 || member.username.search(new RegExp(q.value, 'i')) !== -1
  })
})
</script>

<template>
  <div>
    <UPageCard
      title="Members"
      description="Invite new members by email address."
      variant="naked"
      orientation="horizontal"
      class="mb-4"
    >
      <UButton
        label="Invite people"
        color="neutral"
        class="w-fit lg:ms-auto"
      />
    </UPageCard>

    <UPageCard
      variant="subtle"
      :ui="{ container: 'p-0 sm:p-0 gap-y-0', wrapper: 'items-stretch', header: 'p-4 mb-0 border-b border-default' }"
    >
      <template #header>
        <UInput
          v-model="q"
          icon="i-lucide-search"
          placeholder="Search members"
          autofocus
          class="w-full"
        />
      </template>

      <SettingsMembersList :members="filteredMembers" />
    </UPageCard>
  </div>
</template>
