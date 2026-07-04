<template>
  <aside class="answer-card">
    <div class="answer-card__header">
      <strong>答题卡</strong>
      <span>{{ unansweredCount }} 未答</span>
    </div>
    <div v-for="group in groups" :key="group.id" class="answer-card__section">
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
  id: string
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
  width: 100%;
  padding: clamp(14px, 1.4vw, 18px);
  border: 1px solid var(--ks-border);
  border-radius: var(--ks-radius);
  background: var(--ks-panel);
}

.answer-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.answer-card__header strong {
  min-width: 0;
  overflow-wrap: anywhere;
}

.answer-card__header span {
  flex: none;
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
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.answer-card__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(clamp(30px, 3.3vw, 38px), 1fr));
  gap: clamp(6px, 0.8vw, 8px);
  min-width: 0;
}

.answer-card__item {
  display: grid;
  width: 100%;
  min-width: 0;
  aspect-ratio: 1;
  height: auto;
  min-height: 30px;
  max-height: 40px;
  place-items: center;
  border: 1px solid var(--ks-border);
  border-radius: 6px;
  background: var(--ks-panel-muted);
  color: var(--ks-text-muted);
  font-size: clamp(12px, 1.1vw, 14px);
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
