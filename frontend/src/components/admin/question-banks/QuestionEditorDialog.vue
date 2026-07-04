<template>
  <el-dialog :model-value="visible" :title="editing ? '编辑试题' : '新建试题'" width="860px" @update:model-value="$emit('update:visible', $event)">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
      <el-form-item label="题库" prop="bankId">
        <el-select v-model="form.bankId" filterable class="form-control">
          <el-option v-for="bank in banks" :key="bank.id" :label="bank.name" :value="bank.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="题型" prop="type">
        <el-segmented v-model="form.type" :options="questionTypeOptions" @change="$emit('normalize-options')" />
      </el-form-item>
      <el-form-item label="难度" prop="difficulty">
        <el-select v-model="form.difficulty" class="form-control">
          <el-option label="简单" value="EASY" />
          <el-option label="困难" value="HARD" />
        </el-select>
      </el-form-item>
      <el-form-item label="题干" prop="stem">
        <el-input v-model.trim="form.stem" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item v-if="questionTypeMeta(form.type).optionBased" label="选项">
        <div class="option-editor">
          <div v-for="(option, index) in form.options" :key="`${option.label}-${index}`" class="option-row">
            <el-checkbox v-model="option.correct" />
            <el-input v-model.trim="option.label" class="option-label" />
            <el-input v-model.trim="option.content" placeholder="选项内容" />
            <el-button :icon="Delete" circle :disabled="form.options.length <= 2" @click="$emit('remove-option', index)" />
          </div>
          <el-button :icon="Plus" @click="$emit('add-option')">增加选项</el-button>
        </div>
      </el-form-item>
      <el-form-item label="解析" prop="analysis">
        <el-input v-model.trim="form.analysis" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item label="附件">
        <div class="attachment-editor">
          <el-upload :show-file-list="false" :before-upload="handleUpload" accept=".jpg,.jpeg,.png,.gif,.webp,.mp3,.wav,.ogg,.mp4,.pdf">
            <el-button :icon="Upload" :loading="uploadingAttachment">上传附件</el-button>
          </el-upload>
          <div class="url-attachment">
            <el-input v-model.trim="attachmentUrl" placeholder="输入图片、音频、视频或文件 URL" />
            <el-select v-model="attachmentMediaType" class="url-attachment__type">
              <el-option label="图片" value="IMAGE" />
              <el-option label="音频" value="AUDIO" />
              <el-option label="视频" value="VIDEO" />
              <el-option label="文件" value="FILE" />
            </el-select>
            <el-button @click="$emit('add-url-attachment')">添加 URL</el-button>
          </div>
          <div v-if="form.attachments.length" class="attachment-list">
            <div v-for="(attachment, index) in form.attachments" :key="`${attachment.fileUrl}-${index}`" class="attachment-item">
              <div class="attachment-item__main">
                <el-image
                  v-if="isImageAttachment(attachment)"
                  class="attachment-thumb"
                  :src="attachment.fileUrl"
                  :preview-src-list="[attachment.fileUrl]"
                  fit="cover"
                  preview-teleported
                />
                <el-tag v-else effect="plain">{{ mediaTypeText(attachment.mediaType) }}</el-tag>
                <div class="attachment-meta">
                  <span>{{ attachment.fileName }}</span>
                  <a :href="attachment.fileUrl" target="_blank" rel="noreferrer">
                    {{ isImageAttachment(attachment) ? '查看原图' : '打开附件' }}
                  </a>
                </div>
              </div>
              <div class="attachment-actions">
                <el-button size="small" :disabled="index === 0" @click="$emit('move-attachment', index, -1)">上移</el-button>
                <el-button size="small" :disabled="index === form.attachments.length - 1" @click="$emit('move-attachment', index, 1)">下移</el-button>
                <el-button :icon="Delete" circle @click="$emit('remove-attachment', index)" />
              </div>
            </div>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio-button value="ACTIVE">启用</el-radio-button>
          <el-radio-button value="DISABLED">禁用</el-radio-button>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="$emit('submit')">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules, UploadRawFile } from 'element-plus'
import { Delete, Plus, Upload } from '@element-plus/icons-vue'
import { ref } from 'vue'

import type { QuestionAttachmentPayload, QuestionBank, QuestionPayload } from '@/api/exam-business'
import { isImageAttachment, mediaTypeText } from '@/utils/question-bank-editor'
import { questionTypeMeta, questionTypes } from '@/utils/question-types'

const attachmentUrl = defineModel<string>('attachmentUrl', { required: true })
const attachmentMediaType = defineModel<QuestionAttachmentPayload['mediaType']>('attachmentMediaType', { required: true })

defineProps<{
  visible: boolean
  editing: boolean
  form: QuestionPayload
  rules: FormRules<QuestionPayload>
  banks: QuestionBank[]
  saving: boolean
  uploadingAttachment: boolean
}>()

const emit = defineEmits<{
  'update:visible': [visible: boolean]
  'normalize-options': []
  'add-option': []
  'remove-option': [index: number]
  'add-url-attachment': []
  'remove-attachment': [index: number]
  'move-attachment': [index: number, offset: number]
  upload: [file: UploadRawFile]
  submit: []
}>()

const formRef = ref<FormInstance>()
const questionTypeOptions = questionTypes.map((type) => ({ label: type.shortLabel, value: type.code }))

function handleUpload(file: UploadRawFile) {
  emit('upload', file)
  return false
}

defineExpose({
  validate: () => formRef.value?.validate(),
})
</script>

<style scoped>
.attachment-editor {
  display: grid;
  gap: 10px;
  width: 100%;
}

.url-attachment {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 120px auto;
  gap: 10px;
  align-items: center;
}

.url-attachment__type {
  width: 120px;
}

.attachment-list {
  display: grid;
  gap: 8px;
}

.attachment-item {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  border: 1px solid var(--ks-border);
  border-radius: var(--ks-radius);
}

.attachment-item__main {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.attachment-actions {
  display: flex;
  flex: none;
  align-items: center;
  gap: 6px;
}

.attachment-thumb {
  flex: none;
  width: 72px;
  height: 54px;
  overflow: hidden;
  border: 1px solid var(--ks-border);
  border-radius: 6px;
  background: var(--ks-panel-muted);
}

.attachment-meta {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.attachment-meta span,
.attachment-meta a {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-meta a {
  max-width: 320px;
  color: var(--el-color-primary);
}

@media (max-width: 900px) {
  .url-attachment {
    grid-template-columns: 1fr;
  }
}
</style>
