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
    // 令牌纳入响应式 state：localStorage 仅作为持久化载体。
    // 若直接在 getter 里读 tokenStore（非响应式），守卫首次求值时依赖集为空，
    // 登录后该 getter 不会重算，push 会被守卫拦回 /login（登录成功不跳转）。
    accessToken: tokenStore.access,
  }),

  getters: {
    isLoggedIn: (s) => !!s.accessToken && !!s.user,
    isAdmin: (s) => s.user?.role === 'ADMIN',
  },

  actions: {
    persist(resp: TokenResponse) {
      tokenStore.save(resp.accessToken, resp.refreshToken)
      this.accessToken = resp.accessToken
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
      this.accessToken = null
      this.user = null
      localStorage.removeItem(USER_KEY)
    },

    /** 登录后默认落点 */
    homePath(): string {
      return this.isAdmin ? '/console/apis' : '/portal/dashboard'
    },
  },
})
