import { navigateTo } from '#imports'
import { getTokenCookie } from './util'

type FetchOptions = Parameters<typeof $fetch>[1]

/**
 * `$fetch` com o tratamento de sessão dos sites EHS: 401 limpa o token e volta
 * para o login. 403 é permissão negada e não derruba a sessão.
 */
export async function apiFetch<T = unknown>(url: string, options: FetchOptions = {}): Promise<T> {
  try {
    return await $fetch<T>(url, options)
  } catch (cause) {
    const error = cause as { statusCode?: number, response?: { status?: number } }
    const status = error.statusCode ?? error.response?.status

    if (status === 401 && !url.includes('/auth/login')) {
      getTokenCookie().value = null
      await navigateTo('/auth/login')
    }

    throw cause
  }
}
