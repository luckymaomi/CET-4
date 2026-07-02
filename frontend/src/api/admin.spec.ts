import { describe, expect, it, vi } from 'vitest'

import {
  changeAdminUserStatus,
  createAdminRole,
  createAdminUser,
  createDepartment,
  deleteDepartment,
  downloadUserExport,
  downloadUserImportTemplate,
  fetchAdminMenus,
  fetchAdminPermissions,
  fetchAdminRoles,
  fetchAdminUsers,
  fetchDepartments,
  importUsers,
  updateDepartment,
  updateAdminRole,
  updateAdminUser,
} from './admin'
import { apiClient } from './client'

vi.mock('./client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}))

const ok = <T>(data: T) => ({ data: { code: 0, message: 'OK', data, timestamp: '2026-07-01T00:00:00Z' } })

describe('admin api', () => {
  it('requests paged users with filters', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce(ok({ records: [], total: 0, page: 1, size: 20 }))

    await fetchAdminUsers({ page: 1, size: 20, keyword: 'admin' })

    expect(apiClient.get).toHaveBeenCalledWith('/api/admin/users', {
      params: { page: 1, size: 20, keyword: 'admin' },
    })
  })

  it('writes user and role management payloads through explicit endpoints', async () => {
    vi.mocked(apiClient.post).mockResolvedValue(ok({}))
    vi.mocked(apiClient.put).mockResolvedValue(ok({}))
    vi.mocked(apiClient.patch).mockResolvedValue(ok({}))
    vi.mocked(apiClient.get).mockResolvedValue(ok({}))

    await createAdminUser({ departmentId: 1, username: 'teacher', displayName: '老师', roleIds: [2] })
    await updateAdminUser(2, { departmentId: 2, displayName: '主管', roleIds: [2, 3] })
    await changeAdminUserStatus(2, 'DISABLED')
    await downloadUserImportTemplate()
    await importUsers(new File(['xlsx'], 'users.xlsx'))
    await downloadUserExport()
    await createAdminRole({ code: 'AUDITOR', name: '审计员', description: '', permissionIds: [1], menuIds: [1] })
    await updateAdminRole(4, { code: 'AUDITOR', name: '审计管理员', description: '', permissionIds: [1], menuIds: [1, 2] })

    expect(apiClient.post).toHaveBeenCalledWith('/api/admin/users', {
      departmentId: 1,
      username: 'teacher',
      displayName: '老师',
      roleIds: [2],
    })
    expect(apiClient.put).toHaveBeenCalledWith('/api/admin/users/2', { departmentId: 2, displayName: '主管', roleIds: [2, 3] })
    expect(apiClient.patch).toHaveBeenCalledWith('/api/admin/users/2/status', { status: 'DISABLED' })
    expect(apiClient.get).toHaveBeenCalledWith('/api/admin/users/import-template', { responseType: 'blob' })
    expect(apiClient.post).toHaveBeenCalledWith('/api/admin/users/import', expect.any(FormData))
    expect(apiClient.get).toHaveBeenCalledWith('/api/admin/users/export', { responseType: 'blob' })
    expect(apiClient.post).toHaveBeenCalledWith('/api/admin/roles', {
      code: 'AUDITOR',
      name: '审计员',
      description: '',
      permissionIds: [1],
      menuIds: [1],
    })
    expect(apiClient.put).toHaveBeenCalledWith('/api/admin/roles/4', {
      code: 'AUDITOR',
      name: '审计管理员',
      description: '',
      permissionIds: [1],
      menuIds: [1, 2],
    })
  })

  it('loads role permission and menu dictionaries', async () => {
    vi.mocked(apiClient.get).mockResolvedValue(ok([]))

    await fetchAdminRoles()
    await fetchAdminPermissions()
    await fetchAdminMenus()

    expect(apiClient.get).toHaveBeenCalledWith('/api/admin/roles')
    expect(apiClient.get).toHaveBeenCalledWith('/api/admin/permissions')
    expect(apiClient.get).toHaveBeenCalledWith('/api/admin/menus')
  })

  it('uses department tree crud endpoints', async () => {
    vi.mocked(apiClient.get).mockResolvedValue(ok([]))
    vi.mocked(apiClient.post).mockResolvedValue(ok({}))
    vi.mocked(apiClient.put).mockResolvedValue(ok({}))
    vi.mocked(apiClient.delete).mockResolvedValue(ok({}))

    await fetchDepartments()
    await createDepartment({ parentId: null, name: '部门', code: 'DEPT', description: '', status: 'ACTIVE' })
    await updateDepartment(2, { parentId: 1, name: '部门', code: 'DEPT', description: '', status: 'DISABLED' })
    await deleteDepartment(2)

    expect(apiClient.get).toHaveBeenCalledWith('/api/admin/departments')
    expect(apiClient.post).toHaveBeenCalledWith('/api/admin/departments', {
      parentId: null,
      name: '部门',
      code: 'DEPT',
      description: '',
      status: 'ACTIVE',
    })
    expect(apiClient.put).toHaveBeenCalledWith('/api/admin/departments/2', {
      parentId: 1,
      name: '部门',
      code: 'DEPT',
      description: '',
      status: 'DISABLED',
    })
    expect(apiClient.delete).toHaveBeenCalledWith('/api/admin/departments/2')
  })
})
