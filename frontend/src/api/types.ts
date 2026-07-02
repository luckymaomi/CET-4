export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

export interface CurrentUser {
  id: number
  username: string
  displayName: string
  mustChangePassword: boolean
  roles: string[]
  permissions: string[]
}

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginResult {
  accessToken: string
  tokenType: 'Bearer'
  expiresInSeconds: number
  user: CurrentUser
}
