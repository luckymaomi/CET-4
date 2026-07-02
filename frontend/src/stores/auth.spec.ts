import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { changePassword, fetchCurrentUser, login, logout } from '@/api/auth'
import { useAuthStore } from './auth'

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  changePassword: vi.fn(),
  fetchCurrentUser: vi.fn(),
  logout: vi.fn(),
}))

const currentUser = {
  id: 1,
  username: 'admin',
  displayName: '系统管理员',
  mustChangePassword: false,
  roles: ['ADMIN'],
  permissions: ['system:admin'],
}

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(login).mockReset()
    vi.mocked(changePassword).mockReset()
    vi.mocked(fetchCurrentUser).mockReset()
    vi.mocked(logout).mockReset()
  })

  it('persists token and current user after login', async () => {
    vi.mocked(login).mockResolvedValue({
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresInSeconds: 3600,
      user: currentUser,
    })

    const store = useAuthStore()
    await store.login({ username: 'admin', password: 'password' })

    expect(store.isAuthenticated).toBe(true)
    expect(store.displayName).toBe('系统管理员')
    expect(localStorage.getItem('kaoshi.accessToken')).toBe('access-token')
    expect(JSON.parse(localStorage.getItem('kaoshi.currentUser') || '{}')).toEqual(currentUser)
  })

  it('loads current user with existing token', async () => {
    localStorage.setItem('kaoshi.accessToken', 'access-token')
    vi.mocked(fetchCurrentUser).mockResolvedValue(currentUser)

    const store = useAuthStore()
    await store.loadCurrentUser()

    expect(fetchCurrentUser).toHaveBeenCalledOnce()
    expect(store.user).toEqual(currentUser)
  })

  it('clears local session on logout', async () => {
    localStorage.setItem('kaoshi.accessToken', 'access-token')
    localStorage.setItem('kaoshi.currentUser', JSON.stringify(currentUser))
    vi.mocked(logout).mockResolvedValue(undefined)

    const store = useAuthStore()
    await store.logout()

    expect(store.isAuthenticated).toBe(false)
    expect(store.user).toBeNull()
    expect(localStorage.getItem('kaoshi.accessToken')).toBeNull()
    expect(localStorage.getItem('kaoshi.currentUser')).toBeNull()
  })

  it('refreshes current user after changing password', async () => {
    vi.mocked(changePassword).mockResolvedValue(undefined)
    vi.mocked(fetchCurrentUser).mockResolvedValue(currentUser)

    const store = useAuthStore()
    store.token = 'access-token'
    await store.changePassword({ currentPassword: '123456', newPassword: 'abcdef', confirmPassword: 'abcdef' })

    expect(changePassword).toHaveBeenCalledWith({ currentPassword: '123456', newPassword: 'abcdef', confirmPassword: 'abcdef' })
    expect(fetchCurrentUser).toHaveBeenCalledOnce()
    expect(store.user).toEqual(currentUser)
  })
})

