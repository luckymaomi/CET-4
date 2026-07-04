<template>
  <el-dialog :model-value="visible" :title="editing ? '编辑题库' : '新建题库'" width="520px" @update:model-value="$emit('update:visible', $event)">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="form.categoryId" class="form-control">
          <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model.trim="form.name" maxlength="128" />
      </el-form-item>
      <el-form-item label="说明" prop="description">
        <el-input v-model.trim="form.description" type="textarea" :rows="3" maxlength="500" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-segmented v-model="form.status" :options="statusOptions" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="$emit('submit')">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

import type { NamedCategory, QuestionBankPayload } from '@/api/exam-business'

defineProps<{
  visible: boolean
  editing: boolean
  form: QuestionBankPayload
  rules: FormRules<QuestionBankPayload>
  categories: NamedCategory[]
  saving: boolean
}>()

defineEmits<{
  'update:visible': [visible: boolean]
  submit: []
}>()

const formRef = ref<FormInstance>()

const statusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '禁用', value: 'DISABLED' },
]

defineExpose({
  validate: () => formRef.value?.validate(),
})
</script>
