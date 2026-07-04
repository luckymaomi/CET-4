<template>
  <article :id="panelId" class="question-panel">
    <div class="question-title">
      <strong>{{ index + 1 }}. {{ question.stem }}</strong>
      <div class="question-title__meta">
        <el-tag effect="plain">{{ questionTypeText(question.type) }}</el-tag>
        <el-tag>{{ question.score }} 分</el-tag>
      </div>
    </div>

    <div v-if="question.attachments.length" class="question-media">
      <template v-for="attachment in question.attachments" :key="attachment.id">
        <img
          v-if="attachment.mediaType === 'IMAGE'"
          :src="attachment.fileUrl"
          :alt="attachment.fileName"
          class="question-media__image"
        />
        <audio v-else-if="attachment.mediaType === 'AUDIO'" :src="attachment.fileUrl" controls class="question-media__audio" />
        <el-link v-else :href="attachment.fileUrl" target="_blank">{{ attachment.fileName }}</el-link>
      </template>
    </div>

    <el-checkbox-group
      v-if="question.type === 'MULTIPLE_CHOICE'"
      v-model="multipleAnswers[question.questionId]"
      class="answer-options"
      :disabled="disabled"
      @change="$emit('schedule-save', question)"
    >
      <el-checkbox v-for="option in question.options" :key="option.id" :value="option.label" border>
        {{ option.label }}. {{ option.content }}
      </el-checkbox>
    </el-checkbox-group>
    <el-radio-group
      v-else-if="question.type === 'SINGLE_CHOICE'"
      v-model="singleAnswers[question.questionId]"
      class="answer-options"
      :disabled="disabled"
      @change="$emit('schedule-save', question)"
    >
      <el-radio v-for="option in question.options" :key="option.id" :value="option.label" border>
        {{ option.label }}. {{ option.content }}
      </el-radio>
    </el-radio-group>
    <el-input
      v-else-if="question.type === 'WRITING'"
      v-model="textAnswers[question.questionId]"
      type="textarea"
      :rows="8"
      maxlength="5000"
      show-word-limit
      resize="vertical"
      placeholder="请输入写作答案"
      :disabled="disabled"
      @input="$emit('schedule-save', question)"
      @blur="$emit('save-immediately', question)"
    />
    <footer v-if="$slots.footer" class="question-panel__footer">
      <slot name="footer" />
    </footer>
  </article>
</template>

<script setup lang="ts">
import type { ExamQuestion } from '@/api/exam-business'
import type { MultipleAnswerMap, SingleAnswerMap, TextAnswerMap } from '@/utils/exam-session'
import { questionTypeText } from '@/utils/question-types'

withDefaults(defineProps<{
  question: ExamQuestion
  index: number
  singleAnswers: SingleAnswerMap
  multipleAnswers: MultipleAnswerMap
  textAnswers: TextAnswerMap
  disabled?: boolean
  panelId?: string
}>(), {
  disabled: false,
  panelId: undefined,
})

defineEmits<{
  'schedule-save': [question: ExamQuestion]
  'save-immediately': [question: ExamQuestion]
}>()
</script>

<style scoped>
.question-title__meta {
  display: flex;
  flex: none;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.question-media {
  display: grid;
  gap: 12px;
  margin: 12px 0 16px;
}

.question-media__image {
  width: min(100%, 720px);
  max-height: 360px;
  object-fit: contain;
  border: 1px solid var(--ks-border);
  border-radius: var(--ks-radius);
  background: var(--ks-panel-muted);
}

.question-media__audio {
  width: min(100%, 720px);
}

.answer-options {
  display: grid;
  gap: 10px;
}

.answer-options :deep(.el-checkbox),
.answer-options :deep(.el-radio) {
  width: 100%;
  height: auto;
  margin: 0;
  padding: 12px;
  white-space: normal;
}

.answer-options :deep(.el-checkbox__label),
.answer-options :deep(.el-radio__label) {
  min-width: 0;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.question-panel__footer {
  margin-top: 18px;
}
</style>
