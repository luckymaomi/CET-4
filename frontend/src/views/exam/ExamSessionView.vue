<template>
  <section class="exam-session" v-loading="loading">
    <template v-if="session">
      <header class="exam-status">
        <div>
          <h1>{{ session.title }}</h1>
          <p>{{ answeredCount }} / {{ session.questions.length }} 已作答 · {{ saveStatusText }}</p>
        </div>
        <div class="exam-actions">
          <span class="countdown" :class="{ 'countdown--danger': remainingSeconds <= 300 }">{{ remainingText }}</span>
          <el-button type="primary" :loading="submitting" :disabled="submitted" @click="confirmSubmit">提交试卷</el-button>
        </div>
      </header>

      <section class="exam-workspace">
        <main class="question-stack">
          <article v-if="session.displayMode === 'PAGED' && currentQuestion" :key="currentQuestion.questionId" class="question-panel">
            <div class="question-title">
              <strong>{{ currentIndex + 1 }}. {{ currentQuestion.stem }}</strong>
              <div class="question-title__meta">
                <el-tag effect="plain">{{ questionTypeText(currentQuestion.type) }}</el-tag>
                <el-tag>{{ currentQuestion.score }} 分</el-tag>
              </div>
            </div>

            <div v-if="currentQuestion.attachments.length" class="question-media">
              <template v-for="attachment in currentQuestion.attachments" :key="attachment.id">
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
              v-if="currentQuestion.type === 'MULTIPLE_CHOICE'"
              v-model="multipleAnswers[currentQuestion.questionId]"
              class="answer-options"
              :disabled="submitting || submitted"
              @change="scheduleSave(currentQuestion)"
            >
              <el-checkbox v-for="option in currentQuestion.options" :key="option.id" :value="option.label" border>
                {{ option.label }}. {{ option.content }}
              </el-checkbox>
            </el-checkbox-group>
            <el-radio-group
              v-else-if="currentQuestion.type === 'SINGLE_CHOICE'"
              v-model="singleAnswers[currentQuestion.questionId]"
              class="answer-options"
              :disabled="submitting || submitted"
              @change="scheduleSave(currentQuestion)"
            >
              <el-radio v-for="option in currentQuestion.options" :key="option.id" :value="option.label" border>
                {{ option.label }}. {{ option.content }}
              </el-radio>
            </el-radio-group>
            <el-input
              v-else-if="currentQuestion.type === 'WRITING'"
              v-model="textAnswers[currentQuestion.questionId]"
              type="textarea"
              :rows="8"
              maxlength="5000"
              show-word-limit
              resize="vertical"
              placeholder="请输入写作答案"
              :disabled="submitting || submitted"
              @input="scheduleSave(currentQuestion)"
              @blur="saveImmediately(currentQuestion)"
            />

            <div class="question-nav">
              <el-button :disabled="currentIndex === 0" @click="previousQuestion">上一题</el-button>
              <el-button
                v-if="currentIndex < session.questions.length - 1"
                type="primary"
                @click="nextQuestion"
              >
                下一题
              </el-button>
              <el-button v-else type="primary" :loading="submitting" :disabled="submitted" @click="confirmSubmit">
                提交试卷
              </el-button>
            </div>
          </article>

          <template v-else-if="session.displayMode === 'ALL'">
            <article
              v-for="(question, index) in session.questions"
              :id="`question-${question.questionId}`"
              :key="question.questionId"
              class="question-panel"
            >
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
                :disabled="submitting || submitted"
                @change="scheduleSave(question)"
              >
                <el-checkbox v-for="option in question.options" :key="option.id" :value="option.label" border>
                  {{ option.label }}. {{ option.content }}
                </el-checkbox>
              </el-checkbox-group>
              <el-radio-group
                v-else-if="question.type === 'SINGLE_CHOICE'"
                v-model="singleAnswers[question.questionId]"
                class="answer-options"
                :disabled="submitting || submitted"
                @change="scheduleSave(question)"
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
                :disabled="submitting || submitted"
                @input="scheduleSave(question)"
                @blur="saveImmediately(question)"
              />
            </article>
            <div class="question-submit-row">
              <el-button type="primary" :loading="submitting" :disabled="submitted" @click="confirmSubmit">提交试卷</el-button>
            </div>
          </template>
        </main>

        <aside class="answer-card">
          <div class="answer-card__header">
            <strong>答题卡</strong>
            <span>{{ unansweredCount }} 未答</span>
          </div>
          <div v-for="group in groupedQuestions" :key="group.type" class="answer-card__section">
            <p>{{ group.title }}</p>
            <div class="answer-card__grid">
              <button
                v-for="question in group.questions"
                :key="question.questionId"
                class="answer-card__item"
                :class="{
                  'answer-card__item--answered': isAnswered(question),
                  'answer-card__item--current': currentQuestion?.questionId === question.questionId,
                }"
                type="button"
                @click="selectQuestion(question.questionId)"
              >
                {{ questionGlobalIndex(question.questionId) + 1 }}
              </button>
            </div>
          </div>
        </aside>
      </section>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import { useAnswerSnapshotSaver } from '@/composables/use-answer-snapshot-saver'
import { saveExamAnswers, startExam, submitExam, type ExamQuestion, type ExamSession } from '@/api/exam-business'
import {
  buildSubmitAnswers,
  countAnsweredQuestions,
  formatRemainingTime,
  isQuestionAnswered,
  type MultipleAnswerMap,
  type SingleAnswerMap,
  type TextAnswerMap,
} from '@/utils/exam-session'
import { questionTypeText } from '@/utils/question-types'

const route = useRoute()
const router = useRouter()
const session = ref<ExamSession | null>(null)
const loading = ref(false)
const submitting = ref(false)
const submitted = ref(false)
const remainingSeconds = ref(0)
const currentIndex = ref(0)
let countdownTimer: number | undefined

const multipleAnswers = reactive<MultipleAnswerMap>({})
const singleAnswers = reactive<SingleAnswerMap>({})
const textAnswers = reactive<TextAnswerMap>({})
const answerSaver = useAnswerSnapshotSaver({
  saveSnapshot: async () => {
    if (!session.value) {
      return
    }
    await saveExamAnswers(session.value.examId, buildSubmitAnswers(session.value.questions, singleAnswers, multipleAnswers, textAnswers))
  },
  canSave: () => Boolean(session.value && !submitted.value),
  onAutoSaveError: () => {
    ElMessage.error('答案保存失败，请检查网络后重试')
  },
})

const remainingText = computed(() => formatRemainingTime(remainingSeconds.value))
const currentQuestion = computed(() => session.value?.questions[currentIndex.value] ?? null)
const answeredCount = computed(() => countAnsweredQuestions(session.value?.questions ?? [], singleAnswers, multipleAnswers, textAnswers))
const unansweredCount = computed(() => Math.max(0, (session.value?.questions.length ?? 0) - answeredCount.value))
const hasActiveAttempt = computed(() => Boolean(session.value && !submitted.value))
const saveStatusText = answerSaver.saveStatusText
const groupedQuestions = computed<QuestionGroup[]>(() => {
  const questions = session.value?.questions ?? []
  const groups: QuestionGroup[] = [
    {
      type: 'SINGLE_CHOICE',
      title: '单选题',
      questions: questions.filter((question) => question.type === 'SINGLE_CHOICE'),
    },
    {
      type: 'MULTIPLE_CHOICE',
      title: '多选题',
      questions: questions.filter((question) => question.type === 'MULTIPLE_CHOICE'),
    },
    {
      type: 'WRITING',
      title: '写作题',
      questions: questions.filter((question) => question.type === 'WRITING'),
    },
  ]
  return groups.filter((group) => group.questions.length > 0)
})

interface QuestionGroup {
  type: ExamQuestion['type']
  title: string
  questions: ExamQuestion[]
}

onMounted(() => {
  window.addEventListener('beforeunload', preventUnload)
  void beginExam()
})

onBeforeUnmount(() => {
  stopCountdown()
  answerSaver.clearScheduledSave()
  window.removeEventListener('beforeunload', preventUnload)
})

onBeforeRouteLeave(async () => {
  if (!hasActiveAttempt.value) {
    return true
  }
  if (answerSaver.hasPendingSave.value) {
    try {
      await flushAnswerSaves()
    } catch {
      try {
        await ElMessageBox.confirm('仍有答案保存失败，离开可能丢失最近修改。', '离开考试', {
          confirmButtonText: '离开',
          cancelButtonText: '继续作答',
          type: 'warning',
        })
      } catch {
        return false
      }
    }
  }
  try {
    await ElMessageBox.confirm('离开后本次作答仍在进行，未提交答案不会被锁定。', '离开考试', {
      confirmButtonText: '离开',
      cancelButtonText: '继续作答',
      type: 'warning',
    })
    return true
  } catch {
    return false
  }
})

async function beginExam() {
  loading.value = true
  try {
    const examId = Number(route.params.examId)
    if (!Number.isFinite(examId)) {
      ElMessage.error('考试不存在')
      await router.replace({ name: 'exam-home' })
      return
    }
    session.value = await startExam(examId)
    initializeAnswers(session.value)
    startCountdown(session.value)
  } finally {
    loading.value = false
  }
}

function initializeAnswers(currentSession: ExamSession) {
  for (const question of currentSession.questions) {
    if (question.type === 'MULTIPLE_CHOICE') {
      multipleAnswers[question.questionId] = [...question.selectedLabels]
    } else if (question.type === 'SINGLE_CHOICE') {
      singleAnswers[question.questionId] = question.selectedLabels[0] || ''
    } else if (question.type === 'WRITING') {
      textAnswers[question.questionId] = question.answerText || ''
    }
  }
  answerSaver.lastSavedAt.value = currentSession.questions.some((question) => question.selectedLabels.length > 0 || Boolean(question.answerText?.trim()))
    ? new Date()
    : null
}

function startCountdown(currentSession: ExamSession) {
  stopCountdown()
  const startedAt = new Date(currentSession.startedAt).getTime()
  const deadline = startedAt + currentSession.durationMinutes * 60 * 1000
  const tick = () => {
    remainingSeconds.value = Math.max(0, Math.ceil((deadline - Date.now()) / 1000))
    if (remainingSeconds.value <= 0 && !submitted.value && !submitting.value) {
      void submit(true)
    }
  }
  tick()
  countdownTimer = window.setInterval(tick, 1000)
}

function stopCountdown() {
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
    countdownTimer = undefined
  }
}

function isAnswered(question: ExamQuestion) {
  return isQuestionAnswered(question, singleAnswers, multipleAnswers, textAnswers)
}

function questionGlobalIndex(questionId: number) {
  return session.value?.questions.findIndex((question) => question.questionId === questionId) ?? -1
}

async function selectQuestion(questionId: number) {
  const index = questionGlobalIndex(questionId)
  if (index >= 0) {
    currentIndex.value = index
    if (session.value?.displayMode === 'ALL') {
      await nextTick()
      document.getElementById(`question-${questionId}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }
}

function previousQuestion() {
  currentIndex.value = Math.max(0, currentIndex.value - 1)
}

function nextQuestion() {
  if (!session.value) {
    return
  }
  currentIndex.value = Math.min(session.value.questions.length - 1, currentIndex.value + 1)
}

async function confirmSubmit() {
  const message = unansweredCount.value > 0 ? `还有 ${unansweredCount.value} 题未作答，提交后答案将锁定。` : '提交后答案将锁定。'
  await ElMessageBox.confirm(message, '确认提交', {
    confirmButtonText: '提交',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await submit(false)
}

async function submit(autoSubmit: boolean) {
  if (!session.value || submitted.value || submitting.value) {
    return
  }
  submitting.value = true
  try {
    await flushAnswerSaves()
    const payload = buildSubmitAnswers(session.value.questions, singleAnswers, multipleAnswers, textAnswers)
    const result = await submitExam(session.value.examId, payload)
    submitted.value = true
    stopCountdown()
    ElMessage.success(autoSubmit ? '考试时间已到，试卷已自动提交' : '试卷已提交')
    await router.replace({ name: 'exam-result', params: { resultId: result.id } })
  } finally {
    submitting.value = false
  }
}

function scheduleSave(question: ExamQuestion) {
  answerSaver.scheduleSave(question.questionId)
}

function saveImmediately(question: ExamQuestion) {
  answerSaver.saveImmediately(question.questionId)
}

async function flushAnswerSaves() {
  if (!session.value) {
    return
  }
  await answerSaver.flushSaves(session.value.questions.map((question) => question.questionId))
}

function preventUnload(event: BeforeUnloadEvent) {
  if (!hasActiveAttempt.value || !answerSaver.hasPendingSave.value) {
    return
  }
  event.preventDefault()
  event.returnValue = ''
}

</script>

<style scoped>
.exam-status {
  position: sticky;
  z-index: 5;
  top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-width: 0;
  padding: 18px 20px;
  border: 1px solid var(--ks-border);
  border-radius: var(--ks-radius);
  background: rgb(255 255 255 / 96%);
  box-shadow: var(--ks-shadow);
}

.exam-status h1 {
  margin: 0;
  font-size: 20px;
}

.exam-status p {
  margin: 6px 0 0;
  color: var(--ks-text-muted);
}

.exam-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 16px;
  align-items: start;
}

.exam-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.countdown {
  min-width: 84px;
  color: var(--ks-primary);
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
  font-size: 20px;
  font-weight: 700;
  text-align: right;
}

.countdown--danger {
  color: var(--ks-warning);
}

.question-media {
  display: grid;
  gap: 12px;
  margin: 12px 0 16px;
}

.question-title__meta {
  display: flex;
  flex: none;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
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

.question-nav {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

.question-submit-row {
  display: flex;
  justify-content: flex-end;
  padding: 4px 0 16px;
}

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
  .exam-workspace {
    grid-template-columns: 1fr;
  }

  .answer-card {
    position: static;
  }
}

@media (max-width: 760px) {
  .exam-status,
  .exam-actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
