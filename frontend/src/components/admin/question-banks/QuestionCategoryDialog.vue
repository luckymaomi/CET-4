<template>
  <el-dialog :model-value="visible" :title="editing ? '编辑分类' : '新建分类'" width="480px" @update:model-value="$emit('update:visible', $event)">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
      <el-form-item label="名称" prop="name">
        <el-input v-model.trim="form.name" maxlength="64" />
      </el-form-item>
      <el-form-item label="说明" prop="description">
        <el-input v-model.trim="form.description" type="textarea" :rows="3" maxlength="255" />
      </el-form-item>
      <el-form-item label="排序" prop="sortOrder">
        <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
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

import type { QuestionCategoryPayload } from '@/api/exam-business'

defineProps<{
  visible: boolean
  editing: boolean
  form: QuestionCategoryPayload
  rules: FormRules<QuestionCategoryPayload>
  saving: boolean
}>()

defineEmits<{
  'update:visible': [visible: boolean]
  submit: []
}>()

const formRef = ref<FormInstance>()

defineExpose({
  validate: () => formRef.value?.validate(),
})
</script>
