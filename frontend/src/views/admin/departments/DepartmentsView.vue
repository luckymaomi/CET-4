<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1>部门管理</h1>
      </div>
      <div class="header-actions">
        <el-button type="primary" :icon="Plus" @click="openCreateDialog(null)">新建部门</el-button>
      </div>
    </header>

    <el-table
      v-loading="loading"
      :data="departments"
      row-key="id"
      class="data-table"
      border
      default-expand-all
      :tree-props="{ children: 'children' }"
    >
      <el-table-column label="部门" min-width="240">
        <template #default="{ row }: { row: Department }">
          <div class="entity-stack">
            <span class="entity-name">{{ row.name }}</span>
            <span class="muted-text">{{ row.description || '无说明' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="code" label="部门编码" width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }: { row: Department }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="plain">
            {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="190">
        <template #default="{ row }: { row: Department }">
          <el-button link type="primary" @click="openCreateDialog(row)">新建下级</el-button>
          <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="removeDepartment(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingDepartment ? '编辑部门' : '新建部门'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="上级部门" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            clearable
            class="form-control"
            placeholder="不选择则为顶级部门"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="name">
          <el-input v-model.trim="form.name" maxlength="128" />
        </el-form-item>
        <el-form-item label="部门编码">
          <el-input v-model.trim="form.code" maxlength="64" disabled />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button value="ACTIVE">启用</el-radio-button>
            <el-radio-button value="DISABLED">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model.trim="form.description" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitDepartment">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

import {
  createDepartment,
  deleteDepartment,
  fetchDepartments,
  updateDepartment,
  type Department,
  type DepartmentPayload,
} from '@/api/admin'

const departments = ref<Department[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingDepartment = ref<Department | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<DepartmentPayload>({
  parentId: null,
  name: '',
  code: '',
  description: '',
  status: 'ACTIVE',
})

const rules: FormRules<DepartmentPayload> = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const parentOptions = computed(() => {
  if (!editingDepartment.value) {
    return departments.value
  }
  return withoutDepartment(departments.value, editingDepartment.value.id)
})

onMounted(loadDepartments)

watch(
  () => [form.name, form.parentId, editingDepartment.value?.id] as const,
  () => {
    if (!editingDepartment.value) {
      form.code = generateDepartmentCode(form.name, form.parentId)
    }
  },
)

async function loadDepartments() {
  loading.value = true
  try {
    departments.value = await fetchDepartments()
  } finally {
    loading.value = false
  }
}

function openCreateDialog(parent: Department | null) {
  editingDepartment.value = null
  form.parentId = parent?.id ?? null
  form.name = ''
  form.code = generateDepartmentCode('', parent?.id ?? null)
  form.description = ''
  form.status = 'ACTIVE'
  dialogVisible.value = true
}

function openEditDialog(department: Department) {
  editingDepartment.value = department
  form.parentId = department.parentId
  form.name = department.name
  form.code = department.code
  form.description = department.description || ''
  form.status = department.status
  dialogVisible.value = true
}

async function submitDepartment() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editingDepartment.value) {
      await updateDepartment(editingDepartment.value.id, { ...form })
      ElMessage.success('部门已更新')
    } else {
      await createDepartment({ ...form })
      ElMessage.success('部门已创建')
    }
    dialogVisible.value = false
    await loadDepartments()
  } finally {
    saving.value = false
  }
}

async function removeDepartment(department: Department) {
  await ElMessageBox.confirm(`确认删除“${department.name}”？`, '删除部门', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteDepartment(department.id)
  ElMessage.success('部门已删除')
  await loadDepartments()
}

function withoutDepartment(items: Department[], departmentId: number): Department[] {
  return items
    .filter((item) => item.id !== departmentId)
    .map((item) => ({
      ...item,
      children: withoutDepartment(item.children, departmentId),
    }))
}

function generateDepartmentCode(name: string, parentId: number | null) {
  const prefix = parentId ? `D${parentId}` : 'D'
  const source = name.trim()
  const readable = source
    ? Array.from(source)
        .map((char) => char.charCodeAt(0).toString(36).toUpperCase())
        .join('')
        .slice(0, 18)
    : 'NEW'
  return `${prefix}_${readable}_${Date.now().toString(36).toUpperCase()}`
}
</script>
