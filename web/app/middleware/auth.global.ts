import { useUser } from '~/plugins/userService'
import { getTokenCookie } from '~/utils/util'

const PUBLIC_PATHS = ['/', '/auth/login', '/auth/register']
const AUTH_PATHS = ['/auth/login', '/auth/register']

export default defineNuxtRouteMiddleware(async (to) => {
  const token = getTokenCookie()
  const user = useUser()
  const { $userService } = useNuxtApp()

  if (token.value?.token && !user.value) {
    try {
      user.value = (await $userService.getUser()).data
    } catch {
      token.value = null
    }
  }

  if (AUTH_PATHS.includes(to.path) && user.value) {
    return navigateTo('/dashboard')
  }

  if (PUBLIC_PATHS.includes(to.path)) {
    return
  }

  if (!user.value) {
    return navigateTo('/auth/login')
  }
})
