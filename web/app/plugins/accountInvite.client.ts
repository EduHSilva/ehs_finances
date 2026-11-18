export default defineNuxtPlugin(() => {
  const route = useRoute()
  const token = useState<string>('account-invite-token', () => '')

  watch(() => route.query.invite, (value) => {
    if (!import.meta.client || typeof value !== 'string' || !value.trim()) return
    const next = value.trim()
    token.value = next
    sessionStorage.setItem('finances_account_invite_token', next)
  }, { immediate: true })
})
