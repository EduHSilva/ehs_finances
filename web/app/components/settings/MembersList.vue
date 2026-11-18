<script setup lang="ts">
import type { AccountMember } from '~/types'

defineProps<{
  members: AccountMember[]
  readonly?: boolean
}>()

const emit = defineEmits<{
  resend: [member: AccountMember]
  revoke: [member: AccountMember]
  copy: [member: AccountMember]
}>()

const { t } = useI18n()

function initials(member: AccountMember) {
  const base = (member.name || member.email || '?').trim()
  return base.slice(0, 1).toUpperCase()
}
</script>

<template>
  <ul
    role="list"
    class="divide-y divide-default"
  >
    <li
      v-for="member in members"
      :key="member.id"
      class="flex items-center justify-between gap-3 py-3 px-4 sm:px-6"
    >
      <div class="flex items-center gap-3 min-w-0">
        <UAvatar
          :text="initials(member)"
          size="md"
        />

        <div class="text-sm min-w-0">
          <p class="text-highlighted font-medium truncate">
            {{ member.name || member.email }}
          </p>
          <p class="text-muted truncate">
            {{ member.email }}
          </p>
        </div>
      </div>

      <div class="flex items-center gap-1 sm:gap-2">
        <UBadge
          :color="member.status === 'ACTIVE' ? 'success' : 'warning'"
          variant="subtle"
        >
          {{ member.status === 'ACTIVE' ? t('memberStatusActive') : t('memberStatusPending') }}
        </UBadge>

        <template v-if="!readonly">
          <UButton
            v-if="member.status === 'PENDING' && member.token"
            icon="i-lucide-link"
            color="neutral"
            variant="ghost"
            :aria-label="t('copyInviteLink')"
            :title="t('copyInviteLink')"
            @click="emit('copy', member)"
          />
          <UButton
            v-if="member.status === 'PENDING'"
            icon="i-lucide-send"
            color="neutral"
            variant="ghost"
            :aria-label="t('resendInvite')"
            :title="t('resendInvite')"
            @click="emit('resend', member)"
          />
          <UButton
            icon="i-lucide-x"
            color="error"
            variant="ghost"
            :aria-label="member.status === 'PENDING' ? t('cancelInvite') : t('removeMember')"
            :title="member.status === 'PENDING' ? t('cancelInvite') : t('removeMember')"
            @click="emit('revoke', member)"
          />
        </template>
      </div>
    </li>

    <li
      v-if="!members.length"
      class="px-4 py-6 text-sm text-muted sm:px-6"
    >
      {{ t('noMembers') }}
    </li>
  </ul>
</template>
