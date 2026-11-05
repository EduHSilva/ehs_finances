import type { FinancialAccount, FinancialAccountType } from '~/types'
import { useUser } from '~/plugins/userService'

export type { FinancialAccountType }

export function useFinancialAccounts() {
  const { $financesService } = useNuxtApp()
  const { preferences } = useFinancePreferences()
  const user = useUser()
  const stateKey = `financial-accounts:${user.value?.id || 'anonymous'}`
  const activeAccountId = useCookie(`active-financial-account-${user.value?.id || 'anonymous'}`, { default: () => '' })
  const accounts = useState<FinancialAccount[]>(stateKey, () => [])
  const loaded = useState<boolean>(`${stateKey}:loaded`, () => false)
  const loading = useState<boolean>(`${stateKey}:loading`, () => false)

  const visibleAccounts = computed(() => accounts.value.filter(account =>
    account.type !== 'PJ' || preferences.value.showBusinessAccount
  ))

  const activeAccount = computed<FinancialAccount | null>(() =>
    visibleAccounts.value.find(account => account.id === activeAccountId.value) || visibleAccounts.value[0] || null
  )

  watch(() => [visibleAccounts.value, activeAccountId.value], () => {
    if (activeAccountId.value && !visibleAccounts.value.some(account => account.id === activeAccountId.value)) {
      activeAccountId.value = visibleAccounts.value[0]?.id || ''
    } else if (!activeAccountId.value && visibleAccounts.value[0]) {
      activeAccountId.value = visibleAccounts.value[0].id
    }
  }, { immediate: true })

  async function loadAccounts(force = false) {
    if (loading.value) {
      await new Promise<void>((resolve) => {
        const stop = watch(loading, (isLoading) => {
          if (!isLoading) {
            stop()
            resolve()
          }
        })
      })
      return
    }
    if (loaded.value && !force) return
    loading.value = true
    try {
      const response = await $financesService.getAccounts()
      accounts.value = response.data
      loaded.value = true
    } finally {
      loading.value = false
    }
  }

  const accountOptions = computed(() => visibleAccounts.value.map(account => ({
    label: `${account.type} - ${account.name}`,
    value: account.id
  })))

  function setActiveAccount(accountId: string) {
    if (visibleAccounts.value.some(account => account.id === accountId)) {
      activeAccountId.value = accountId
    }
  }

  return { accounts, visibleAccounts, accountOptions, activeAccount, setActiveAccount, loadAccounts, loading }
}
