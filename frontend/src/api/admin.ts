import { apiClient } from './client'
import type { ApiResponse } from './types'

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface AdminUser {
  id: number
  departmentId: number | null
  departmentName: string | null
  username: string
  displayName: string
  status: 'ACTIVE' | 'DISABLED'
  roles: string[]
  createdAt: string
  updatedAt: string
}

export interface UserCreatePayload {
  departmentId: number | null
  username: string
  displayName: string
  roleIds: number[]
}

export interface UserUpdatePayload {
  departmentId: number | null
  displayName: string
  roleIds: number[]
}

export interface AdminRole {
  id: number
  code: string
  name: string
  description: string | null
  permissions: AdminPermission[]
  menus: AdminMenu[]
}

export interface RoleSavePayload {
  code: string
  name: string
  description: string
  permissionIds: number[]
  menuIds: number[]
}

export interface AdminPermission {
  id: number
  code: string
  name: string
  description: string | null
}

export interface AdminMenu {
  id: number
  code: string
  title: string
  path: string
  parentId: number | null
  sortOrder: number
  icon: string | null
}

export interface ExcelImportResult {
  successCount: number
  failureCount: number
  errors: string[]
}

export interface Department {
  id: number
  parentId: number | null
  name: string
  code: string
  description: string | null
  status: 'ACTIVE' | 'DISABLED'
  children: Department[]
}

export interface DepartmentPayload {
  parentId: number | null
  name: string
  code: string
  description: string
  status: Department['status']
}

export async function fetchAdminUsers(params: {
  page: number
  size: number
  keyword?: string
}): Promise<PageResult<AdminUser>> {
  const response = await apiClient.get<ApiResponse<PageResult<AdminUser>>>('/api/admin/users', { params })
  return response.data.data
}

export async function createAdminUser(payload: UserCreatePayload): Promise<AdminUser> {
  const response = await apiClient.post<ApiResponse<AdminUser>>('/api/admin/users', payload)
  return response.data.data
}

export async function updateAdminUser(id: number, payload: UserUpdatePayload): Promise<AdminUser> {
  const response = await apiClient.put<ApiResponse<AdminUser>>(`/api/admin/users/${id}`, payload)
  return response.data.data
}

export async function changeAdminUserStatus(id: number, status: AdminUser['status']): Promise<AdminUser> {
  const response = await apiClient.patch<ApiResponse<AdminUser>>(`/api/admin/users/${id}/status`, { status })
  return response.data.data
}

export async function downloadUserImportTemplate(): Promise<Blob> {
  const response = await apiClient.get('/api/admin/users/import-template', { responseType: 'blob' })
  return response.data
}

export async function importUsers(file: File): Promise<ExcelImportResult> {
  const form = new FormData()
  form.append('file', file)
  const response = await apiClient.post<ApiResponse<ExcelImportResult>>('/api/admin/users/import', form)
  return response.data.data
}

export async function downloadUserExport(): Promise<Blob> {
  const response = await apiClient.get('/api/admin/users/export', { responseType: 'blob' })
  return response.data
}

export async function fetchAdminRoles(): Promise<AdminRole[]> {
  const response = await apiClient.get<ApiResponse<AdminRole[]>>('/api/admin/roles')
  return response.data.data
}

export async function createAdminRole(payload: RoleSavePayload): Promise<AdminRole> {
  const response = await apiClient.post<ApiResponse<AdminRole>>('/api/admin/roles', payload)
  return response.data.data
}

export async function updateAdminRole(id: number, payload: RoleSavePayload): Promise<AdminRole> {
  const response = await apiClient.put<ApiResponse<AdminRole>>(`/api/admin/roles/${id}`, payload)
  return response.data.data
}

export async function fetchAdminPermissions(): Promise<AdminPermission[]> {
  const response = await apiClient.get<ApiResponse<AdminPermission[]>>('/api/admin/permissions')
  return response.data.data
}

export async function fetchAdminMenus(): Promise<AdminMenu[]> {
  const response = await apiClient.get<ApiResponse<AdminMenu[]>>('/api/admin/menus')
  return response.data.data
}

export async function fetchDepartments(): Promise<Department[]> {
  const response = await apiClient.get<ApiResponse<Department[]>>('/api/admin/departments')
  return response.data.data
}

export async function createDepartment(payload: DepartmentPayload): Promise<Department> {
  const response = await apiClient.post<ApiResponse<Department>>('/api/admin/departments', payload)
  return response.data.data
}

export async function updateDepartment(id: number, payload: DepartmentPayload): Promise<Department> {
  const response = await apiClient.put<ApiResponse<Department>>(`/api/admin/departments/${id}`, payload)
  return response.data.data
}

export async function deleteDepartment(id: number): Promise<void> {
  await apiClient.delete<ApiResponse<void>>(`/api/admin/departments/${id}`)
}
