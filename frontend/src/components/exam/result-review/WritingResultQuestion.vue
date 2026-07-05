<template>
  <article class="question-panel">
    <div class="question-title">
      <strong>{{ questionTitle }}</strong>
      <div class="question-title__meta">
        <el-tag effect="plain">{{ questionTypeText(question.type) }}</el-tag>
        <el-tag :type="scoreTagType">{{ question.obtainedScore }} / {{ question.score }} 分</el-tag>
      </div>
    </div>

    <QuestionMedia :attachments="question.attachments" />

    <div class="writing-answer">
      <span>{{ questionTypeText(question.type) }}答案</span>
      <p>{{ question.answerText || '未作答' }}</p>
    </div>

    <div v-if="reviewable" class="writing-review-form">
      <el-input-number v-model="form.score" :min="0" :max="question.score" :step="0.5" :controls="false" />
      <el-input
        v-model.trim="form.comment"
        type="textarea"
        :rows="3"
        maxlength="1000"
        show-word-limit
        placeholder="阅卷评语"
      />
      <el-button type="primary" :loading="saving" @click="$emit('save', question)">保存评分</el-button>
    </div>

    <div class="review-summary">
      <span>评分状态：{{ question.reviewedAt ? '已保存评分' : '待阅卷' }}</span>
      <span>阅卷人：{{ question.reviewerName || '未记录' }}</span>
      <span>阅卷时间：{{ question.reviewedAt ? formatDateTime(question.reviewedAt) : '未完成' }}</span>
      <span v-if="question.reviewComment">评语：{{ question.reviewComment }}</span>
    </div>

    <p v-if="question.analysis" class="analysis-text">解析：{{ question.analysis }}</p>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { ExamResultQuestion } from '@/api/exam-business'
import { formatDateTime } from '@/utils/datetime'
import { questionTypeText } from '@/utils/question-types'
import QuestionMedia from './QuestionMedia.vue'

const props = defineProps<{
  question: ExamResultQuestion
  index: number
  form: { score: number; comment: string }
  reviewable: boolean
  saving: boolean
}>()

defineEmits<{
  save: [question: ExamResultQuestion]
}>()

const scoreTagType = computed(() => {
  if (!props.question.reviewedAt) {
    return 'warning'
  }
  return props.question.correct === false ? 'danger' : 'success'
})

const questionTitle = computed(() => {
  const label = String(props.index + 1)
  const stem = props.question.stem
  return stem ? `${label}. ${stem}` : label
})
</script>

<style scoped>
.question-title__meta {
  display: flex;
  flex: none;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.review-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  margin-top: 14px;
  color: var(--ks-text-muted);
  font-size: 13px;
}

.writing-answer {
  display: grid;
  gap: 8px;
  margin-top: 14px;
  padding: 12px;
  border: 1px solid var(--ks-border);
  border-radius: var(--ks-radius);
  background: var(--ks-panel-muted);
}

.writing-answer span {
  color: var(--ks-text-muted);
  font-size: 13px;
}

.writing-answer p {
  margin: 0;
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.writing-review-form {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: start;
  margin-top: 12px;
}

.writing-review-form :deep(.el-input-number) {
  width: 100%;
}

.analysis-text {
  margin: 14px 0 0;
  color: var(--ks-text);
  line-height: 1.7;
  overflow-wrap: anywhere;
}

@media (max-width: 900px) {
  .writing-review-form {
    grid-template-columns: 1fr;
  }
}
</style>
