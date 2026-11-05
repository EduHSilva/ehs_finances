import userService from '@/services/userService'
import type { User } from '~/utils/types'

export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()

  const service = userService({
    apiUrl: `${config.public.apiUrl || apiOrigin()}/users/`
  })

  return {
    provide: {
      userService: service
    }
  }
})

export const useUser = () => useState<User | null>('user', () => null)
