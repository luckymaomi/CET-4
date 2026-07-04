<template>
  <section class="metric-grid">
    <article class="metric">
      <span>得分</span>
      <strong>{{ result.obtainedScore }}</strong>
    </article>
    <article class="metric">
      <span>总分</span>
      <strong>{{ result.totalScore }}</strong>
    </article>
    <article class="metric">
      <span>正确题数</span>
      <strong>{{ result.correctCount }} / {{ result.questionCount }}</strong>
    </article>
    <article class="metric">
      <span>客观题分</span>
      <strong>{{ result.objectiveScore }}</strong>
    </article>
    <article class="metric">
      <span>主观题分</span>
      <strong>{{ result.subjectiveScore }}</strong>
    </article>
    <article class="metric">
      <span>阅卷状态</span>
      <strong>{{ result.gradingStatus === 'FINAL' ? '已出分' : '待阅卷' }}</strong>
    </article>
  </section>

  <section v-if="reviewable && result.gradingStatus === 'PENDING_REVIEW'" class="review-toolbar">
    <span>保存单题评分后可以退出，重新进入会读回已保存分数和评语；完成阅卷会锁定最终成绩。</span>
    <el-button type="primary" :loading="completing" :disabled="!allWritingReviewed" @click="completeReview">完成阅卷</el-button>
  </section>

  <section class="result-review">
    <article v-for="(question, index) in result.questions" :key="question.questionId" class="question-panel">
      <div class="question-title">
        <strong>{{ index + 1 }}. {{ question.stem }}</strong>
        <div class="question-title__meta">
          <el-tag effect="plain">{{ questionTypeText(question.type) }}</el-tag>
          <el-tag :type="scoreTagType(question)">{{ question.obtainedScore }} / {{ question.score }} 分</el-tag>
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

      <template v-if="question.type === 'WRITING'">
        <div class="writing-answer">
          <span>写作答案</span>
          <p>{{ question.answerText || '未作答' }}</p>
        </div>

        <div v-if="reviewable && result.gradingStatus === 'PENDING_REVIEW'" class="writing-review-form">
          <el-input-number
            v-model="reviewForms[question.questionId].score"
            :min="0"
            :max="question.score"
            :step="0.5"
            :controls="false"
          />
          <el-input
            v-model.trim="reviewForms[question.questionId].comment"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            placeholder="阅卷评语"
          />
          <el-button type="primary" :loading="savingQuestionId === question.questionId" @click="saveReview(question)">保存评分</el-button>
        </div>

        <div class="review-summary">
          <span>评分状态：{{ question.reviewedAt ? '已保存评分' : '待阅卷' }}</span>
          <span>阅卷人：{{ question.reviewerName || '未记录' }}</span>
          <span>阅卷时间：{{ question.reviewedAt ? formatDateTime(question.reviewedAt) : '未完成' }}</span>
          <span v-if="question.reviewComment">评语：{{ question.reviewComment }}</span>
        </div>
      </template>

      <template v-else>
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
      </template>

      <p v-if="question.analysis" class="analysis-text">解析：{{ question.analysis }}</p>
    </article>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

import {
  completeResultReview,
  reviewWritingQuestion,
  type ExamResultDetail,
  type ExamResultQuestion,
} from '@/api/exam-business'
import { formatDateTime } from '@/utils/datetime'
import { questionTypeText } from '@/utils/question-types'

const props = withDefaults(defineProps<{
  result: ExamResultDetail
  reviewable?: boolean
}>(), {
  reviewable: false,
})

const emit = defineEmits<{
  updated: [result: ExamResultDetail]
}>()

const completing = ref(false)
const savingQuestionId = ref<number | null>(null)
const reviewForms = reactive<Record<number, { score: number; comment: string }>>({})
const writingQuestions = computed(() => props.result.questions.filter((question) => question.type === 'WRITING'))
const allWritingReviewed = computed(() => writingQuestions.value.length > 0 && writingQuestions.value.every((question) => Boolean(question.reviewedAt)))

watch(
  () => props.result,
  (result) => {
    for (const question of result.questions) {
      if (question.type !== 'WRITING') {
        continue
      }
      reviewForms[question.questionId] = {
        score: question.obtainedScore,
        comment: question.reviewComment || '',
      }
    }
  },
  { immediate: true },
)

function labelsText(labels: string[]) {
  return labels.length ? labels.join('、') : '未作答'
}

function scoreTagType(question: ExamResultQuestion) {
  if (question.type === 'WRITING' && !question.reviewedAt) {
    return 'warning'
  }
  return question.correct === false ? 'danger' : 'success'
}

async function saveReview(question: ExamResultQuestion) {
  const form = reviewForms[question.questionId]
  if (!form) {
    return
  }
  savingQuestionId.value = question.questionId
  try {
    const updated = await reviewWritingQuestion(props.result.id, question.questionId, {
      score: form.score,
      comment: form.comment,
    })
    emit('updated', updated)
    ElMessage.success('评分已保存')
  } finally {
    savingQuestionId.value = null
  }
}

async function completeReview() {
  completing.value = true
  try {
    const updated = await completeResultReview(props.result.id)
    emit('updated', updated)
    ElMessage.success('阅卷已完成')
  } finally {
    completing.value = false
  }
}
</script>

<style scoped>
.review-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  margin-bottom: 16px;
  padding: 12px 14px;
  border: 1px solid var(--ks-border);
  border-radius: var(--ks-radius);
  background: var(--ks-panel-muted);
}

.review-toolbar span {
  color: var(--ks-text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.result-review {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.question-title__meta {
  display: flex;
  flex: none;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.question-media {
  display: grid;
  gap: 10px;
  margin: 12px 0;
}

.question-media__image {
  display: block;
  max-width: min(520px, 100%);
  max-height: 280px;
  border: 1px solid var(--ks-border);
  border-radius: var(--ks-radius);
  object-fit: contain;
}

.question-media__audio {
  width: min(520px, 100%);
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
  .review-toolbar,
  .writing-review-form {
    display: grid;
    grid-template-columns: 1fr;
  }

  .review-toolbar {
    align-items: stretch;
  }
}
</style>
