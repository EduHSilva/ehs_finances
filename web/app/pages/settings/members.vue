<script setup lang="ts">
import type { AccountMember, AccountMembers } from '~/types'
import MembersList from '~/components/settings/MembersList.vue'

const { $financesService } = useNuxtApp()
const { t } = useI18n()
const toast = useToast()
const { activeAccount, accountOptions, loadAccounts, setActiveAccount, loading: accountsLoading } = useFinancialAccounts()
const invites = useAccountInvites()
const {
  token: inviteToken,
  invites: incomingInvites,
  pending: invitePending,
  feedback: inviteFeedback,
  error: inviteActionError,
  load: loadInvites,
  accept: acceptInvite,
  decline: declineInvite
} = invites

const summary = ref<AccountMembers | null>(null)
const members = ref<AccountMember[]>([])
const loadingMembers = ref(false)
const membersError = ref('')
const search = ref('')
const lastInviteLink = ref('')

const inviteOpen = ref(false)
const inviteEmail = ref('')
const inviting = ref(false)
const inviteFormError = ref('')

const revokeOpen = ref(false)
const revokeTarget = ref<AccountMember | null>(null)
const revoking = ref(false)

const isOwner = computed(() => !!activeAccount.value && (activeAccount.value.role ?? 'OWNER') === 'OWNER')
const selectedAccountId = computed(() => activeAccount.value?.id ?? '')
const ownerLabel = computed(() => summary.value?.ownerName || summary.value?.ownerEmail || t('accountOwner'))

const filteredMembers = computed(() => {
  const term = search.value.trim().toLowerCase()
  if (!term) return members.value
  return members.value.filter(member =>
    (member.name || '').toLowerCase().includes(term) || member.email.toLowerCase().includes(term)
  )
})

function errorText(cause: unknown, fallback: string) {
  const err = cause as { data?: { message?: string }, statusMessage?: string }
  return err.data?.message || err.statusMessage || fallback
}

function inviteLink(token: string) {
  if (!import.meta.client) return ''
  const base = useRuntimeConfig().app.baseURL || '/'
  return `${window.location.origin}${base}?invite=${token}`
}

async function loadMembers() {
  if (!selectedAccountId.value) {
    summary.value = null
    members.value = []
    return
  }
  loadingMembers.value = true
  membersError.value = ''
  try {
    const response = await $financesService.getAccountMembers(selectedAccountId.value)
    summary.value = response.data
    members.value = response.data.members ?? []
  } catch (cause) {
    membersError.value = errorText(cause, t('membersLoadError'))
  } finally {
    loadingMembers.value = false
  }
}

function changeAccount(value: string) {
  lastInviteLink.value = ''
  setActiveAccount(value)
}

function openInvite() {
  inviteEmail.value = ''
  inviteFormError.value = ''
  inviteOpen.value = true
}

async function submitInvite() {
  if (!selectedAccountId.value || inviting.value) return
  const email = inviteEmail.value.trim()
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    inviteFormError.value = t('invalidEmail')
    return
  }
  inviting.value = true
  inviteFormError.value = ''
  try {
    const response = await $financesService.inviteAccountMember(selectedAccountId.value, email)
    if (response.data?.token) lastInviteLink.value = inviteLink(response.data.token)
    inviteOpen.value = false
    toast.add({ title: t('inviteSent'), color: 'success' })
    await loadMembers()
  } catch (cause) {
    inviteFormError.value = errorText(cause, t('inviteSendError'))
  } finally {
    inviting.value = false
  }
}

async function copyText(link: string) {
  try {
    await navigator.clipboard.writeText(link)
    toast.add({ title: t('inviteLinkCopied'), color: 'success' })
  } catch {
    lastInviteLink.value = link
  }
}

async function copyInvite(member: AccountMember) {
  if (!member.token) return
  await copyText(inviteLink(member.token))
}

async function resendInvite(member: AccountMember) {
  if (!selectedAccountId.value) return
  try {
    const response = await $financesService.resendAccountInvite(selectedAccountId.value, member.id)
    if (response.data?.token) lastInviteLink.value = inviteLink(response.data.token)
    toast.add({ title: t('inviteResent'), color: 'success' })
    await loadMembers()
  } catch (cause) {
    toast.add({ title: errorText(cause, t('inviteSendError')), color: 'error' })
  }
}

function askRevoke(member: AccountMember) {
  revokeTarget.value = member
  revokeOpen.value = true
}

async function confirmRevoke() {
  if (!selectedAccountId.value || !revokeTarget.value || revoking.value) return
  revoking.value = true
  try {
    await $financesService.revokeAccountMember(selectedAccountId.value, revokeTarget.value.id)
    toast.add({ title: t('memberRemoved'), color: 'success' })
    revokeOpen.value = false
    await loadMembers()
  } catch (cause) {
    toast.add({ title: errorText(cause, t('memberRemoveError')), color: 'error' })
  } finally {
    revoking.value = false
  }
}

async function acceptSharedInvite() {
  if (await acceptInvite()) {
    toast.add({ title: t('inviteAccepted'), color: 'success' })
    await loadAccounts(true)
    await loadMembers()
  }
}

async function declineSharedInvite() {
  if (await declineInvite()) {
    toast.add({ title: t('inviteDeclined'), color: 'neutral' })
  }
}

watch(selectedAccountId, () => loadMembers())

onMounted(async () => {
  try {
    await loadAccounts()
  } catch {
    // O erro de conta já aparece na tela de membros como lista vazia.
  }
  await Promise.all([loadMembers(), loadInvites()])
})
</script>

<template>
  <div class="flex flex-col gap-4">
    <UAlert
      v-if="inviteToken"
      color="primary"
      variant="subtle"
      icon="i-lucide-users"
      :title="t('sharedInviteReceived')"
      :description="t('sharedInviteReceivedDesc')"
    >
      <template #actions>
        <UButton
          :label="t('acceptInvite')"
          :loading="invitePending"
          @click="acceptSharedInvite"
        />
        <UButton
          :label="t('declineInvite')"
          color="neutral"
          variant="subtle"
          :disabled="invitePending"
          @click="declineSharedInvite"
        />
      </template>
    </UAlert>

    <UAlert
      v-if="inviteFeedback"
      color="success"
      variant="subtle"
      icon="i-lucide-check"
      :title="inviteFeedback"
    />

    <UAlert
      v-if="inviteActionError"
      color="error"
      variant="subtle"
      icon="i-lucide-triangle-alert"
      :title="inviteActionError"
    />

    <UPageCard
      v-if="accountOptions.length > 1"
      variant="subtle"
      :title="t('sharedAccount')"
      :description="t('sharedAccountSelectHelp')"
      orientation="horizontal"
    >
      <USelect
        :model-value="selectedAccountId"
        :items="accountOptions"
        :loading="accountsLoading"
        class="w-full lg:w-64 lg:ms-auto"
        @update:model-value="changeAccount"
      />
    </UPageCard>

    <UPageCard
      variant="naked"
      :title="summary?.accountName || t('members')"
      :description="t('membersDescription')"
      orientation="horizontal"
    >
      <UButton
        v-if="isOwner"
        :label="t('invitePeople')"
        icon="i-lucide-user-plus"
        color="neutral"
        class="w-fit lg:ms-auto"
        @click="openInvite"
      />
    </UPageCard>

    <UAlert
      v-if="activeAccount && !isOwner"
      color="neutral"
      variant="subtle"
      icon="i-lucide-info"
      :title="t('sharedAccountReadOnly')"
    />

    <UAlert
      v-if="lastInviteLink"
      color="primary"
      variant="subtle"
      icon="i-lucide-link"
      :title="t('inviteLinkReady')"
      :description="lastInviteLink"
    >
      <template #actions>
        <UButton
          :label="t('copyInviteLink')"
          icon="i-lucide-copy"
          color="neutral"
          variant="subtle"
          @click="copyText(lastInviteLink)"
        />
      </template>
    </UAlert>

    <UAlert
      v-if="membersError"
      color="error"
      variant="subtle"
      icon="i-lucide-triangle-alert"
      :title="membersError"
    />

    <UPageCard
      variant="subtle"
      :ui="{ container: 'p-0 sm:p-0 gap-y-0', wrapper: 'items-stretch', header: 'p-4 mb-0 border-b border-default' }"
    >
      <template #header>
        <div class="flex w-full flex-col gap-2 sm:flex-row sm:items-center">
          <UInput
            v-model="search"
            icon="i-lucide-search"
            :placeholder="t('searchMembers')"
            class="w-full"
          />
          <p class="text-sm whitespace-nowrap text-muted">
            {{ t('accountOwnerLabel') }}: {{ ownerLabel }}
          </p>
        </div>
      </template>

      <div
        v-if="loadingMembers"
        class="px-4 py-6 text-sm text-muted sm:px-6"
      >
        {{ t('loading') }}
      </div>
      <MembersList
        v-else
        :members="filteredMembers"
        :readonly="!isOwner"
        @copy="copyInvite"
        @resend="resendInvite"
        @revoke="askRevoke"
      />
    </UPageCard>

    <UPageCard
      v-if="incomingInvites.length"
      variant="subtle"
      :title="t('incomingInvites')"
      :description="t('incomingInvitesDesc')"
    >
      <ul class="divide-y divide-default">
        <li
          v-for="invite in incomingInvites"
          :key="invite.id"
          class="flex items-center justify-between gap-3 py-3"
        >
          <div class="min-w-0 text-sm">
            <p class="text-highlighted font-medium truncate">
              {{ invite.accountName }}
            </p>
            <p class="text-muted truncate">
              {{ t('inviteFrom', { owner: invite.ownerName || invite.ownerEmail || t('accountOwner') }) }}
            </p>
          </div>
          <UBadge
            color="warning"
            variant="subtle"
          >
            {{ t('memberStatusPending') }}
          </UBadge>
        </li>
      </ul>
    </UPageCard>

    <UModal
      v-model:open="inviteOpen"
      :title="t('inviteMemberTitle')"
      :description="t('inviteMemberDesc')"
    >
      <template #body>
        <form
          class="space-y-4"
          @submit.prevent="submitInvite"
        >
          <UFormField
            :label="t('email')"
            name="email"
          >
            <UInput
              v-model="inviteEmail"
              type="email"
              class="w-full"
              :placeholder="t('emailPlaceholder')"
              autofocus
            />
          </UFormField>

          <p
            v-if="inviteFormError"
            class="text-sm text-error"
          >
            {{ inviteFormError }}
          </p>

          <div class="flex justify-end gap-2">
            <UButton
              :label="t('cancel')"
              color="neutral"
              variant="subtle"
              @click="inviteOpen = false"
            />
            <UButton
              :label="t('sendInvite')"
              type="submit"
              :loading="inviting"
            />
          </div>
        </form>
      </template>
    </UModal>

    <UModal
      v-model:open="revokeOpen"
      :title="revokeTarget?.status === 'PENDING' ? t('cancelInvite') : t('removeMember')"
      :description="revokeTarget?.status === 'PENDING' ? t('cancelInviteDesc') : t('removeMemberDesc')"
    >
      <template #body>
        <div class="flex justify-end gap-2">
          <UButton
            :label="t('cancel')"
            color="neutral"
            variant="subtle"
            @click="revokeOpen = false"
          />
          <UButton
            :label="revokeTarget?.status === 'PENDING' ? t('cancelInvite') : t('removeMember')"
            color="error"
            :loading="revoking"
            @click="confirmRevoke"
          />
        </div>
      </template>
    </UModal>
  </div>
</template>
