import { defineStore } from 'pinia'

import { changePassword, fetchCurrentUser, login, logout } from '@/api/auth'
import type { CurrentUser, LoginPayload } from '@/api/types'

const TOKEN_KEY = 'kaoshi.accessToken'
const USER_KEY = 'kaoshi.currentUser'

interface AuthState {
  token: string
  user: CurrentUser | null
  loading: boolean
}

function readUser(): CurrentUser | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as CurrentUser
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readUser(),
    loading: false,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    displayName: (state) => state.user?.displayName || state.user?.username || '',
    permissions: (state) => state.user?.permissions || [],
  },
  actions: {
    async login(payload: LoginPayload) {
      this.loading = true
      try {
        const result = await login(payload)
        this.token = result.accessToken
        this.user = result.user
        localStorage.setItem(TOKEN_KEY, result.accessToken)
        localStorage.setItem(USER_KEY, JSON.stringify(result.user))
      } finally {
        this.loading = false
      }
    },
    async loadCurrentUser() {
      if (!this.token) {
        return
      }
      this.user = await fetchCurrentUser()
      localStorage.setItem(USER_KEY, JSON.stringify(this.user))
    },
    async changePassword(payload: { currentPassword: string; newPassword: string; confirmPassword: string }) {
      await changePassword(payload)
      await this.loadCurrentUser()
    },
    async logout() {
      if (this.token) {
        await logout().catch(() => undefined)
      }
      this.clearSession()
    },
    clearSession() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
  },
})

