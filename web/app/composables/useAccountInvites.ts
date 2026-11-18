import type { IncomingAccountInvite } from '~/types'

const INVITE_KEY = 'finances_account_invite_token'

export function useAccountInvites() {
  const { $financesService } = useNuxtApp()
  const { t } = useI18n()
  const token = useState<string>('account-invite-token', () => '')
  const invites = useState<IncomingAccountInvite[]>('account-invites', () => [])
  const loading = useState<boolean>('account-invites-loading', () => false)
  const pending = useState<boolean>('account-invites-pending', () => false)
  const feedback = useState<string>('account-invites-feedback', () => '')
  const error = useState<string>('account-invites-error', () => '')

  function restoreToken() {
    if (!import.meta.client) return
    if (!token.value) token.value = sessionStorage.getItem(INVITE_KEY) ?? ''
  }

  function captureToken(value: unknown) {
    if (!import.meta.client || typeof value !== 'string') return
    const next = value.trim()
    if (!next) return
    token.value = next
    sessionStorage.setItem(INVITE_KEY, next)
  }

  function clearToken() {
    if (import.meta.client) sessionStorage.removeItem(INVITE_KEY)
    token.value = ''
  }

  async function load() {
    loading.value = true
    try {
      invites.value = (await $financesService.getAccountInvites()).data ?? []
    } catch {
      invites.value = []
    } finally {
      loading.value = false
    }
  }

  async function accept() {
    if (pending.value || !token.value) return false
    pending.value = true
    error.value = ''
    feedback.value = ''
    try {
      await $financesService.acceptAccountInvite(token.value)
      clearToken()
      feedback.value = t('inviteAccepted')
      await load()
      return true
    } catch (cause) {
      const err = cause as { data?: { message?: string }, statusMessage?: string }
      error.value = err.data?.message || err.statusMessage || t('inviteAcceptError')
      return false
    } finally {
      pending.value = false
    }
  }

  async function decline() {
    if (pending.value || !token.value) return false
    pending.value = true
    error.value = ''
    feedback.value = ''
    try {
      await $financesService.declineAccountInvite(token.value)
      clearToken()
      feedback.value = t('inviteDeclined')
      await load()
      return true
    } catch (cause) {
      const err = cause as { data?: { message?: string }, statusMessage?: string }
      error.value = err.data?.message || err.statusMessage || t('inviteDeclineError')
      return false
    } finally {
      pending.value = false
    }
  }

  void restoreToken()

  return { token, invites, loading, pending, feedback, error, captureToken, clearToken, load, accept, decline }
}
