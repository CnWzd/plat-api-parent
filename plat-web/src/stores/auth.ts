import { defineStore } from 'pinia'
import { authApi, type LoginPayload, type RegisterPayload } from '@/api'
import { tokenStore } from '@/api/client'
import type { AuthUser, TokenResponse } from '@/types'

const USER_KEY = 'plat.user'

function loadUser(): AuthUser | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AuthUser
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: loadUser(),
  }),

  getters: {
    isLoggedIn: (s) => !!tokenStore.access && !!s.user,
    isAdmin: (s) => s.user?.role === 'ADMIN',
  },

  actions: {
    persist(resp: TokenResponse) {
      tokenStore.save(resp.accessToken, resp.refreshToken)
      this.user = resp.user
      localStorage.setItem(USER_KEY, JSON.stringify(resp.user))
    },

    async login(payload: LoginPayload) {
      this.persist(await authApi.login(payload))
    },

    async register(payload: RegisterPayload) {
      this.persist(await authApi.register(payload))
    },

    async fetchMe() {
      const resp = await authApi.me()
      this.persist(resp)
    },

    logout() {
      authApi.logout()
      this.user = null
      localStorage.removeItem(USER_KEY)
    },

    /** 登录后默认落点 */
    homePath(): string {
      return this.isAdmin ? '/console/apis' : '/portal/dashboard'
    },
  },
})
