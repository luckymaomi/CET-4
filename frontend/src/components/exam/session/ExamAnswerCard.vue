<template>
  <aside class="answer-card">
    <div class="answer-card__header">
      <strong>答题卡</strong>
      <span>{{ unansweredCount }} 未答</span>
    </div>
    <div v-for="group in groups" :key="group.type" class="answer-card__section">
      <p>{{ group.title }}</p>
      <div class="answer-card__grid">
        <button
          v-for="question in group.questions"
          :key="question.questionId"
          class="answer-card__item"
          :class="{
            'answer-card__item--answered': isAnswered(question),
            'answer-card__item--current': currentQuestionId === question.questionId,
          }"
          type="button"
          @click="$emit('select-question', question.questionId)"
        >
          {{ questionIndex(question.questionId) + 1 }}
        </button>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import type { ExamQuestion } from '@/api/exam-business'

export interface AnswerCardGroup {
  type: ExamQuestion['type']
  title: string
  questions: ExamQuestion[]
}

const props = defineProps<{
  groups: AnswerCardGroup[]
  questions: ExamQuestion[]
  unansweredCount: number
  currentQuestionId?: number
  isAnswered: (question: ExamQuestion) => boolean
}>()

defineEmits<{
  'select-question': [questionId: number]
}>()

function questionIndex(questionId: number) {
  return props.questions.findIndex((question) => question.questionId === questionId)
}
</script>

<style scoped>
.answer-card {
  position: sticky;
  top: 92px;
  display: grid;
  gap: 14px;
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--ks-border);
  border-radius: var(--ks-radius);
  background: var(--ks-panel);
}

.answer-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.answer-card__header span {
  color: var(--ks-text-muted);
  font-size: 13px;
}

.answer-card__section {
  display: grid;
  gap: 8px;
}

.answer-card__section p {
  margin: 0;
  color: var(--ks-text-muted);
  font-size: 13px;
  font-weight: 600;
}

.answer-card__grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.answer-card__item {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border: 1px solid var(--ks-border);
  border-radius: 6px;
  background: var(--ks-panel-muted);
  color: var(--ks-text-muted);
  cursor: pointer;
}

.answer-card__item--answered {
  border-color: var(--ks-success);
  background: #ecfdf3;
  color: #027a48;
}

.answer-card__item--current {
  border-color: var(--ks-primary);
  background: var(--ks-primary-soft);
  color: var(--ks-primary);
  font-weight: 700;
}

@media (max-width: 900px) {
  .answer-card {
    position: static;
  }
}
</style>
