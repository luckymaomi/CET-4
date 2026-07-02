import { apiClient } from './client'
import type { ApiResponse, CurrentUser, LoginPayload, LoginResult } from './types'

export async function login(payload: LoginPayload): Promise<LoginResult> {
  const response = await apiClient.post<ApiResponse<LoginResult>>('/api/auth/login', payload)
  return response.data.data
}

export async function fetchCurrentUser(): Promise<CurrentUser> {
  const response = await apiClient.get<ApiResponse<CurrentUser>>('/api/auth/me')
  return response.data.data
}

export async function logout(): Promise<void> {
  await apiClient.post<ApiResponse<void>>('/api/auth/logout')
}

export async function changePassword(payload: {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}): Promise<void> {
  await apiClient.post<ApiResponse<void>>('/api/auth/change-password', payload)
}
