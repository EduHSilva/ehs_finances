import type { FinancePreferences } from '~/types'
import { useUser } from '~/plugins/userService'

const defaultPreferences: FinancePreferences = {
  showBusinessAccount: true,
  currency: 'BRL'
}

export function useFinancePreferences() {
  const { $financesService } = useNuxtApp()
  const user = useUser()
  const stateKey = `finance-preferences:${user.value?.id || 'anonymous'}`
  const preferences = useState<FinancePreferences>(stateKey, () => ({ ...defaultPreferences }))
  const loaded = useState<boolean>(`${stateKey}:loaded`, () => false)
  const loading = useState<boolean>('finance-preferences-loading', () => false)
  const saving = useState<boolean>('finance-preferences-saving', () => false)

  function cacheKey() {
    return `finance-preferences:${user.value?.id || 'anonymous'}`
  }

  function readCache(): FinancePreferences | null {
    if (!import.meta.client) return null
    try {
      const raw = localStorage.getItem(cacheKey())
      if (!raw) return null
      const parsed = JSON.parse(raw) as Partial<FinancePreferences>
      if (typeof parsed.showBusinessAccount !== 'boolean') return null
      return {
        showBusinessAccount: parsed.showBusinessAccount,
        currency: typeof parsed.currency === 'string' ? parsed.currency : 'BRL'
      }
    } catch {
      return null
    }
  }

  function writeCache(value: FinancePreferences) {
    if (!import.meta.client) return
    try {
      localStorage.setItem(cacheKey(), JSON.stringify(value))
    } catch {
      // Storage can be unavailable in private browsing; the in-memory cache remains usable.
    }
  }

  async function load(force = false) {
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
    const cached = readCache()
    if (cached) preferences.value = cached
    try {
      const response = await $financesService.getPreferences()
      preferences.value = { ...defaultPreferences, ...response.data }
      writeCache(preferences.value)
      loaded.value = true
    } catch (error) {
      if (!cached) throw error
      loaded.value = true
    } finally {
      loading.value = false
    }
  }

  async function save(value: FinancePreferences) {
    saving.value = true
    try {
      const response = await $financesService.updatePreferences(value)
      preferences.value = { ...defaultPreferences, ...response.data }
      writeCache(preferences.value)
      loaded.value = true
      return preferences.value
    } finally {
      saving.value = false
    }
  }

  return { preferences, loaded, loading, saving, load, save }
}
