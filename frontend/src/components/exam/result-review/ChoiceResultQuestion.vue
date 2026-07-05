<template>
  <article class="question-panel">
    <div class="question-title">
      <strong>{{ questionTitle }}</strong>
      <div class="question-title__meta">
        <el-tag effect="plain">{{ questionTypeText(question.type) }}</el-tag>
        <el-tag :type="question.correct === false ? 'danger' : 'success'">{{ question.obtainedScore }} / {{ question.score }} 分</el-tag>
      </div>
    </div>

    <QuestionMedia :attachments="question.attachments" />

    <div class="review-options">
      <div
        v-for="option in question.options"
        :key="option.id"
        class="review-option"
        :class="{
          'review-option--selected': question.selectedLabels.includes(option.label),
          'review-option--correct': question.correctLabels.includes(option.label),
        }"
      >
        <span>{{ option.label }}. {{ option.content }}</span>
        <el-tag v-if="question.correctLabels.includes(option.label)" size="small" type="success">正确答案</el-tag>
        <el-tag v-else-if="question.selectedLabels.includes(option.label)" size="small" type="warning">我的答案</el-tag>
      </div>
    </div>

    <div class="review-summary">
      <span>我的答案：{{ labelsText(question.selectedLabels) }}</span>
      <span>正确答案：{{ labelsText(question.correctLabels) }}</span>
      <span>结果：{{ question.correct ? '正确' : '错误' }}</span>
    </div>

    <p v-if="question.analysis" class="analysis-text">解析：{{ question.analysis }}</p>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { ExamResultQuestion } from '@/api/exam-business'
import { questionTypeText } from '@/utils/question-types'
import QuestionMedia from './QuestionMedia.vue'

const props = defineProps<{
  question: ExamResultQuestion
  index: number
}>()

const questionTitle = computed(() => {
  const label = String(props.index + 1)
  const stem = props.question.stem
  return stem ? `${label}. ${stem}` : label
})

function labelsText(labels: string[]) {
  return labels.length ? labels.join('、') : '未作答'
}
</script>

<style scoped>
.question-title__meta {
  display: flex;
  flex: none;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.review-options {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.review-option {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--ks-border);
  border-radius: var(--ks-radius);
  background: var(--ks-panel-muted);
  line-height: 1.5;
}

.review-option span {
  min-width: 0;
  overflow-wrap: anywhere;
}

.review-option--selected {
  border-color: var(--ks-warning);
  background: #fffaeb;
}

.review-option--correct {
  border-color: var(--ks-success);
  background: #ecfdf3;
}

.review-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  margin-top: 14px;
  color: var(--ks-text-muted);
  font-size: 13px;
}

.analysis-text {
  margin: 14px 0 0;
  color: var(--ks-text);
  line-height: 1.7;
  overflow-wrap: anywhere;
}
</style>
