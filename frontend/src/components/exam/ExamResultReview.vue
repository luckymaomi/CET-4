<template>
  <ResultMetricGrid :result="result" />

  <section v-if="canReviewWriting" class="review-toolbar">
    <span>保存单题评分后可以退出，重新进入会读回已保存分数和评语；完成阅卷会锁定最终成绩。</span>
    <el-button type="primary" :loading="completing" :disabled="!allWritingReviewed" @click="completeReview">完成阅卷</el-button>
  </section>

  <section class="result-review">
    <section v-for="group in resultQuestionGroups" :key="group.id" class="result-review__group">
      <h2>{{ group.title }}</h2>
      <template v-for="question in group.questions" :key="question.questionId">
        <WritingResultQuestion
          v-if="isManualReviewType(question.type)"
          :question="question"
          :index="questionIndex(question.questionId)"
          :form="reviewForms[question.questionId]"
          :reviewable="canReviewWriting"
          :saving="savingQuestionId === question.questionId"
          @save="saveReview"
        />
        <ChoiceResultQuestion
          v-else
          :question="question"
          :index="questionIndex(question.questionId)"
        />
      </template>
    </section>
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
import { isManualReviewType, questionTypeText } from '@/utils/question-types'
import ChoiceResultQuestion from './result-review/ChoiceResultQuestion.vue'
import ResultMetricGrid from './result-review/ResultMetricGrid.vue'
import WritingResultQuestion from './result-review/WritingResultQuestion.vue'

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
const writingQuestions = computed(() => props.result.questions.filter((question) => isManualReviewType(question.type)))
const canReviewWriting = computed(() => props.reviewable && props.result.gradingStatus === 'PENDING_REVIEW')
const allWritingReviewed = computed(() => writingQuestions.value.length > 0 && writingQuestions.value.every((question) => Boolean(question.reviewedAt)))
const resultQuestionGroups = computed(() => groupQuestionsByType(props.result.questions))

watch(
  () => props.result,
  (result) => {
    for (const question of result.questions) {
      if (!isManualReviewType(question.type)) {
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

function questionIndex(questionId: number) {
  return props.result.questions.findIndex((question) => question.questionId === questionId)
}

function groupQuestionsByType(questions: ExamResultQuestion[]) {
  const typeOrder: ExamResultQuestion['type'][] = ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'WRITING']
  return typeOrder
    .map((type) => ({
      id: type,
      title: questionTypeText(type),
      questions: questions.filter((question) => question.type === type),
    }))
    .filter((group) => group.questions.length > 0)
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

.result-review__group {
  display: grid;
  gap: 14px;
}

.result-review__group h2 {
  margin: 0;
  font-size: 16px;
}

@media (max-width: 900px) {
  .review-toolbar {
    display: grid;
    align-items: stretch;
  }
}
</style>
