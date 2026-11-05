import type { DefaultResponse, User } from '~/utils/types'
import { getTokenCookie } from '~/utils/util'

export interface UserServiceOptions {
  apiUrl: string | unknown
}

export interface UserService {
  login(email: string, password: string): Promise<DefaultResponse<User>>
  getUser(): Promise<DefaultResponse<User>>
  register(username: string, email: string, password: string): Promise<DefaultResponse<User>>
}

export default function userService({ apiUrl }: UserServiceOptions): UserService {
  const baseUrl = String(apiUrl)
  return {
    login: (email, password) => $fetch(`${baseUrl}auth/login`, {
      method: 'POST', body: { email, password }
    }),
    getUser: () => $fetch(`${baseUrl}auth/me`, {
      headers: { Authorization: `Bearer ${getTokenCookie().value?.token || ''}` }
    }),
    register: (username, email, password) => $fetch(baseUrl, {
      method: 'POST', body: { username, email, password, role: 'CRM' }
    })
  }
}
