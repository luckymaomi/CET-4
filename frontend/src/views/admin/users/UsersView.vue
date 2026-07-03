<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1>用户管理</h1>
      </div>
      <div class="header-actions">
        <el-button :icon="Download" @click="downloadTemplate">下载模板</el-button>
        <el-upload :show-file-list="false" accept=".xlsx" :before-upload="handleImport">
          <el-button :icon="Upload">导入用户</el-button>
        </el-upload>
        <el-button :icon="Download" @click="exportUsers">导出用户</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建用户</el-button>
      </div>
    </header>

    <div class="toolbar">
      <el-input
        v-model.trim="query.keyword"
        clearable
        placeholder="搜索账号或姓名"
        class="toolbar__search"
        @keyup.enter="loadUsers"
        @clear="loadUsers"
      />
      <el-button :icon="Search" @click="loadUsers">搜索</el-button>
    </div>

    <el-alert v-if="importResult" :type="importResult.failureCount ? 'warning' : 'success'" show-icon :closable="false">
      <template #title>
        导入完成：成功 {{ importResult.successCount }} 条，失败 {{ importResult.failureCount }} 条
      </template>
      <ul v-if="importResult.errors.length" class="import-errors">
        <li v-for="error in importResult.errors" :key="error">{{ error }}</li>
      </ul>
    </el-alert>

    <el-table v-loading="loading" :data="users" class="data-table" border>
      <el-table-column prop="username" label="账号" min-width="140" />
      <el-table-column prop="displayName" label="姓名" min-width="140" />
      <el-table-column label="部门" min-width="140">
        <template #default="{ row }: { row: AdminUser }">
          {{ row.departmentName || '未分配' }}
        </template>
      </el-table-column>
      <el-table-column label="角色" min-width="220">
        <template #default="{ row }: { row: AdminUser }">
          <el-space wrap>
            <el-tag v-for="role in row.roles" :key="role" effect="plain">{{ roleName(role) }}</el-tag>
          </el-space>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }: { row: AdminUser }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="220">
        <template #default="{ row }: { row: AdminUser }">
          <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
          <el-button
            link
            :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
            :disabled="row.id === 1 && row.status === 'ACTIVE'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-row">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadUsers"
        @current-change="loadUsers"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="editingUser ? '编辑用户' : '新建用户'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item v-if="!editingUser" label="账号" prop="username">
          <el-input v-model.trim="form.username" maxlength="64" />
        </el-form-item>
        <el-form-item label="姓名" prop="displayName">
          <el-input v-model.trim="form.displayName" maxlength="64" />
        </el-form-item>
        <el-form-item label="部门">
          <el-tree-select
            v-model="form.departmentId"
            :data="departments"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            clearable
            class="form-control"
            placeholder="选择部门"
          />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple class="form-control" placeholder="选择角色">
            <el-option v-for="role in roles" :key="role.id" :label="`${role.name} (${role.code})`" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitUser">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadRawFile } from 'element-plus'
import { Download, Plus, Search, Upload } from '@element-plus/icons-vue'

import {
  changeAdminUserStatus,
  createAdminUser,
  downloadUserExport,
  downloadUserImportTemplate,
  fetchDepartments,
  fetchAdminRoles,
  fetchAdminUsers,
  importUsers,
  updateAdminUser,
  type Department,
  type AdminRole,
  type AdminUser,
  type ExcelImportResult,
} from '@/api/admin'
import { downloadBlob } from '@/utils/download'

interface UserForm {
  username: string
  departmentId: number | null
  displayName: string
  roleIds: number[]
}

const users = ref<AdminUser[]>([])
const roles = ref<AdminRole[]>([])
const departments = ref<Department[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingUser = ref<AdminUser | null>(null)
const importResult = ref<ExcelImportResult | null>(null)
const formRef = ref<FormInstance>()

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
})

const form = reactive<UserForm>({
  username: '',
  departmentId: null,
  displayName: '',
  roleIds: [],
})

const rules: FormRules<UserForm> = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  displayName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleIds: [{ required: true, type: 'array', min: 1, message: '请选择角色', trigger: 'change' }],
}

onMounted(async () => {
  await Promise.all([loadDepartments(), loadRoles(), loadUsers()])
})

async function loadDepartments() {
  departments.value = await fetchDepartments()
}

async function loadRoles() {
  roles.value = await fetchAdminRoles()
}

async function loadUsers() {
  loading.value = true
  try {
    const result = await fetchAdminUsers({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
    })
    users.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingUser.value = null
  form.username = ''
  form.departmentId = null
  form.displayName = ''
  form.roleIds = []
  dialogVisible.value = true
}

function openEditDialog(user: AdminUser) {
  editingUser.value = user
  form.username = user.username
  form.departmentId = user.departmentId
  form.displayName = user.displayName
  form.roleIds = roles.value.filter((role) => user.roles.includes(role.code)).map((role) => role.id)
  dialogVisible.value = true
}

async function submitUser() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editingUser.value) {
      await updateAdminUser(editingUser.value.id, {
        departmentId: form.departmentId,
        displayName: form.displayName,
        roleIds: form.roleIds,
      })
      ElMessage.success('用户已更新')
    } else {
      await createAdminUser({
        departmentId: form.departmentId,
        username: form.username,
        displayName: form.displayName,
        roleIds: form.roleIds,
      })
      ElMessage.success('用户已创建')
    }
    dialogVisible.value = false
    await loadUsers()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(user: AdminUser) {
  await changeAdminUserStatus(user.id, user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE')
  ElMessage.success('用户状态已更新')
  await loadUsers()
}

async function downloadTemplate() {
  const blob = await downloadUserImportTemplate()
  downloadBlob(blob, '用户导入模板.xlsx')
}

async function exportUsers() {
  const blob = await downloadUserExport()
  downloadBlob(blob, '用户导出.xlsx')
}

async function handleImport(file: UploadRawFile) {
  importResult.value = await importUsers(file)
  await loadUsers()
  return false
}

function roleName(code: string) {
  return roles.value.find((role) => role.code === code)?.name || code
}
</script>

<style scoped>
.import-errors {
  margin: 8px 0 0;
  padding-left: 18px;
}
</style>
