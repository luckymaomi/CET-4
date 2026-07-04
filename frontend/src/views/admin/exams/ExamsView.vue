<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1>考试管理</h1>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateEditor">新建考试</el-button>
    </header>

    <div class="toolbar">
      <el-input v-model.trim="query.keyword" clearable placeholder="搜索考试名称" class="toolbar__search" @keyup.enter="loadExams" />
      <el-button :icon="Search" @click="loadExams">搜索</el-button>
    </div>

    <el-table v-loading="loading" :data="exams" class="data-table" border>
      <el-table-column label="考试名称" min-width="220">
        <template #default="{ row }: { row: Exam }">
          <span class="entity-name">{{ row.title }}</span>
        </template>
      </el-table-column>
      <el-table-column label="组卷规模" width="140">
        <template #default="{ row }: { row: Exam }">
          {{ row.questionCount }} 题 / {{ row.totalScore }} 分
        </template>
      </el-table-column>
      <el-table-column label="及格分" width="100">
        <template #default="{ row }: { row: Exam }">
          {{ row.qualifyScore }}
        </template>
      </el-table-column>
      <el-table-column label="时间" min-width="230">
        <template #default="{ row }: { row: Exam }">
          <div class="entity-stack">
            <span>{{ row.durationMinutes }} 分钟</span>
            <span class="muted-text">{{ row.timeLimit ? `${formatDateTime(row.startTime)} 至 ${formatDateTime(row.endTime)}` : '不限考试日期' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="开放范围" width="120">
        <template #default="{ row }: { row: Exam }">
          <el-tag effect="plain" :type="row.openType === 'PUBLIC' ? 'success' : 'warning'">
            {{ row.openType === 'PUBLIC' ? '公开' : '部门' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="题目显示" width="150">
        <template #default="{ row }: { row: Exam }">
          <div class="entity-stack">
            <span>{{ displayModeText(row.displayMode) }}</span>
            <span class="muted-text">{{ questionOrderText(row.questionOrderMode) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="可考次数" width="110">
        <template #default="{ row }: { row: Exam }">
          {{ row.attemptLimit ? `${row.attemptLimit} 次` : '无限次' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }: { row: Exam }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="350">
        <template #default="{ row }: { row: Exam }">
          <el-button link type="primary" @click="openEditEditor(row)">编辑</el-button>
          <el-button link type="primary" @click="openResults(row)">成绩</el-button>
          <el-button link type="primary" @click="copyCurrentExam(row)">复制</el-button>
          <el-button link type="primary" @click="downloadCurrentExam(row)">下载</el-button>
          <el-button v-if="row.status === 'PUBLISHED'" link type="warning" @click="revokeCurrentExam(row)">撤销发布</el-button>
          <el-button link type="danger" @click="deleteCurrentExam(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-row">
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="loadExams" />
    </div>

    <ExamEditorDialog
      ref="formRef"
      v-model:visible="editorVisible"
      v-model:ruleset="ruleset"
      v-model:paper-questions="paperQuestions"
      v-model:time-range="timeRange"
      v-model:attempt-limit-mode="attemptLimitMode"
      v-model:limited-attempt-count="limitedAttemptCount"
      :editing-exam="editingExam"
      :current-status="currentStatus"
      :form="form"
      :form-rules="formRules"
      :total-score="totalScore"
      :total-question-count="totalQuestionCount"
      :banks="banks"
      :bank-questions="bankQuestions"
      :picker="picker"
      :picker-questions="pickerQuestions"
      :picker-loading="pickerLoading"
      :departments="departments"
      :saving="saving"
      :publishing="publishing"
      :closing="closing"
      @add-rule="addRule"
      @remove-rule="removeRule"
      @rule-bank-change="onRuleBankChange"
      @mark-paper-stale="markPaperStale"
      @generate-paper="generatePaperQuestions"
      @preview-paper="previewVisible = true"
      @load-picker-questions="loadPickerQuestions"
      @add-manual-question="addManualQuestion"
      @sort-paper="sortPaperQuestions"
      @move-paper-question="movePaperQuestion"
      @remove-paper-question="removePaperQuestion"
      @close-exam="closeCurrentExam"
      @save-draft="saveDraft"
      @publish-exam="publishCurrentExam"
    />

    <ExamPaperPreviewDialog v-model:visible="previewVisible" :questions="paperQuestions" />
    <ExamResultsDrawer
      v-model:visible="resultsVisible"
      :exam="selectedResultExam"
      :results="examResults"
      :loading="resultsLoading"
      @open-detail="openResultDetail"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormRules } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

import ExamEditorDialog from '@/components/admin/exams/ExamEditorDialog.vue'
import ExamPaperPreviewDialog from '@/components/admin/exams/ExamPaperPreviewDialog.vue'
import ExamResultsDrawer from '@/components/admin/exams/ExamResultsDrawer.vue'
import { fetchDepartments, type Department } from '@/api/admin'
import {
  closeExam,
  copyExam,
  createExam,
  deleteExam,
  downloadExamPaper,
  fetchAdminExamDetail,
  fetchAdminExams,
  fetchAdminResults,
  fetchQuestionBanks,
  fetchQuestions,
  publishExam,
  revokeExam,
  updateExam,
  type Exam,
  type ExamPayload,
  type ExamResult,
  type Question,
  type QuestionBank,
} from '@/api/exam-business'
import { downloadBlob } from '@/utils/download'
import { formatDateTime } from '@/utils/datetime'
import {
  calculateTotalQuestionCount,
  calculateTotalScore,
  createDefaultRule,
  defaultManualScore as calculateDefaultManualScore,
  examRulesToForms,
  generatePaperQuestionsFromRules,
  normalizePaperSort,
  toExamPaperQuestionPayloads,
  toExamRulePayloads,
  toGeneratedPaperQuestion,
  toPaperQuestionForm,
  type ExamPaperQuestionForm,
  type ExamRuleForm,
} from '@/utils/admin-exam-editor'

const router = useRouter()
const exams = ref<Exam[]>([])
const banks = ref<QuestionBank[]>([])
const departments = ref<Department[]>([])
const bankQuestions = ref<Record<number, Question[]>>({})
const ruleset = ref<ExamRuleForm[]>([])
const paperQuestions = ref<ExamPaperQuestionForm[]>([])
const pickerQuestions = ref<Question[]>([])
const examResults = ref<ExamResult[]>([])
const paperStale = ref(false)
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const closing = ref(false)
const pickerLoading = ref(false)
const resultsLoading = ref(false)
const previewVisible = ref(false)
const resultsVisible = ref(false)
const editorVisible = ref(false)
const editingExam = ref<Exam | null>(null)
const selectedResultExam = ref<Exam | null>(null)
const currentStatus = ref<Exam['status']>('DRAFT')
const formRef = ref<{ validate: () => Promise<boolean> | undefined }>()
const timeRange = ref<[string, string]>(['2026-01-01T00:00:00', '2026-12-31T23:59:59'])
const attemptLimitMode = ref<'UNLIMITED' | 'LIMITED'>('UNLIMITED')
const limitedAttemptCount = ref<number | null>(1)
const picker = reactive({ bankId: null as number | null, keyword: '' })

const query = reactive({ page: 1, size: 20, keyword: '' })
const form = reactive<ExamPayload>({
  title: '',
  description: '',
  qualifyScore: 0,
  startTime: '2026-01-01T00:00:00',
  endTime: '2026-12-31T23:59:59',
  durationMinutes: 30,
  timeLimit: true,
  attemptLimit: null,
  displayMode: 'PAGED',
  questionOrderMode: 'FIXED',
  openType: 'PUBLIC',
  departmentIds: [],
  rules: [],
  paperQuestions: [],
})

const formRules: FormRules<ExamPayload> = {
  title: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
  qualifyScore: [{ required: true, message: '请输入及格分', trigger: 'change' }],
  durationMinutes: [{ required: true, message: '请输入考试时长', trigger: 'change' }],
  displayMode: [{ required: true, message: '请选择题目显示方式', trigger: 'change' }],
  questionOrderMode: [{ required: true, message: '请选择题目顺序', trigger: 'change' }],
  openType: [{ required: true, message: '请选择开放范围', trigger: 'change' }],
}

const totalScore = computed(() => calculateTotalScore(ruleset.value, paperQuestions.value))
const totalQuestionCount = computed(() => calculateTotalQuestionCount(ruleset.value, paperQuestions.value))

onMounted(async () => {
  await Promise.all([loadBanks(), loadDepartments(), loadExams()])
})

async function loadBanks() {
  const result = await fetchQuestionBanks({ page: 1, size: 200 })
  banks.value = result.records
  picker.bankId = picker.bankId || banks.value[0]?.id || null
}

async function loadDepartments() {
  departments.value = await fetchDepartments()
}

async function loadExams() {
  loading.value = true
  try {
    const result = await fetchAdminExams({ page: query.page, size: query.size, keyword: query.keyword || undefined })
    exams.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function openCreateEditor() {
  editingExam.value = null
  resetForm()
  addRule()
  editorVisible.value = true
}

async function openEditEditor(exam: Exam) {
  resetForm()
  const detail = await fetchAdminExamDetail(exam.id)
  await fillEditor(detail)
  editorVisible.value = true
}

async function fillEditor(exam: Exam) {
  editingExam.value = exam
  currentStatus.value = exam.status
  form.title = exam.title
  form.description = exam.description || ''
  form.qualifyScore = exam.qualifyScore
  form.startTime = exam.startTime
  form.endTime = exam.endTime
  form.durationMinutes = exam.durationMinutes
  form.timeLimit = exam.timeLimit
  form.attemptLimit = exam.attemptLimit
  form.displayMode = exam.displayMode
  form.questionOrderMode = exam.questionOrderMode
  form.openType = exam.openType
  form.departmentIds = [...exam.departmentIds]
  attemptLimitMode.value = exam.attemptLimit ? 'LIMITED' : 'UNLIMITED'
  limitedAttemptCount.value = exam.attemptLimit || 1
  timeRange.value = [exam.startTime, exam.endTime]
  ruleset.value = examRulesToForms(exam)
  for (const rule of ruleset.value) {
    await loadBankQuestions(rule.bankId)
  }
  paperQuestions.value = exam.paperQuestions.map(toPaperQuestionForm)
  paperStale.value = false
}

function resetForm() {
  ruleset.value = []
  form.title = ''
  form.description = ''
  form.qualifyScore = 0
  form.startTime = '2026-01-01T00:00:00'
  form.endTime = '2026-12-31T23:59:59'
  form.durationMinutes = 30
  form.timeLimit = true
  form.attemptLimit = null
  form.displayMode = 'PAGED'
  form.questionOrderMode = 'FIXED'
  form.openType = 'PUBLIC'
  form.departmentIds = []
  form.rules = []
  form.paperQuestions = []
  paperQuestions.value = []
  pickerQuestions.value = []
  paperStale.value = false
  previewVisible.value = false
  currentStatus.value = 'DRAFT'
  attemptLimitMode.value = 'UNLIMITED'
  limitedAttemptCount.value = 1
  timeRange.value = [form.startTime, form.endTime]
}

function addRule() {
  ruleset.value.push(createDefaultRule(Date.now() + ruleset.value.length))
  markPaperStale()
}

function removeRule(index: number) {
  ruleset.value.splice(index, 1)
  markPaperStale()
}

async function loadBankQuestions(bankId: number | null, force = false) {
  if (!bankId || (!force && bankQuestions.value[bankId])) {
    return
  }
  const result = await fetchQuestions({ page: 1, size: 500, bankId })
  bankQuestions.value = {
    ...bankQuestions.value,
    [bankId]: result.records.filter((question) => question.status === 'ACTIVE'),
  }
}

async function onRuleBankChange(bankId: number | null) {
  await loadBankQuestions(bankId)
  picker.bankId = bankId || picker.bankId
  markPaperStale()
}

function markPaperStale() {
  paperStale.value = true
}

async function generatePaperQuestions() {
  for (const rule of ruleset.value) {
    await loadBankQuestions(rule.bankId, true)
  }
  paperQuestions.value = generatePaperQuestionsFromRules(ruleset.value, bankQuestions.value)
  paperStale.value = false
}

async function loadPickerQuestions() {
  if (!picker.bankId) {
    pickerQuestions.value = []
    return
  }
  pickerLoading.value = true
  try {
    const result = await fetchQuestions({
      page: 1,
      size: 100,
      bankId: picker.bankId,
      keyword: picker.keyword || undefined,
    })
    pickerQuestions.value = result.records.filter((question) => question.status === 'ACTIVE')
  } finally {
    pickerLoading.value = false
  }
}

function addManualQuestion(question: Question) {
  if (hasPaperQuestion(question.id)) {
      return
  }
  paperQuestions.value = normalizePaperSort([
    ...paperQuestions.value,
    toGeneratedPaperQuestion(question, defaultManualScore(question.type), (paperQuestions.value.length + 1) * 10),
  ])
}

function hasPaperQuestion(questionId: number) {
  return paperQuestions.value.some((question) => question.questionId === questionId)
}

function defaultManualScore(type: Question['type']) {
  return calculateDefaultManualScore(type, ruleset.value, picker.bankId)
}

function movePaperQuestion(index: number, offset: number) {
  const target = index + offset
  if (target < 0 || target >= paperQuestions.value.length) {
    return
  }
  const rows = [...paperQuestions.value]
  const [current] = rows.splice(index, 1)
  rows.splice(target, 0, current)
  paperQuestions.value = normalizePaperSort(rows)
}

function sortPaperQuestions() {
  paperQuestions.value = normalizePaperSort([...paperQuestions.value].sort((left, right) => left.sortOrder - right.sortOrder))
}

function removePaperQuestion(index: number) {
  const rows = [...paperQuestions.value]
  rows.splice(index, 1)
  paperQuestions.value = normalizePaperSort(rows)
}

async function saveDraft() {
  await formRef.value?.validate()
  const payload = await buildPayload()
  if (!payload) {
    return
  }
  saving.value = true
  try {
    const saved = editingExam.value ? await updateExam(editingExam.value.id, payload) : await createExam(payload)
    const detail = await fetchAdminExamDetail(saved.id)
    await fillEditor(detail)
    ElMessage.success('草稿已保存')
    await loadExams()
  } finally {
    saving.value = false
  }
}

async function publishCurrentExam() {
  if (!editingExam.value) {
    ElMessage.warning('请先保存草稿后再发布')
    return
  }
  publishing.value = true
  try {
    const published = await publishExam(editingExam.value.id)
    await fillEditor(await fetchAdminExamDetail(published.id))
    ElMessage.success('考试已发布')
    await loadExams()
  } finally {
    publishing.value = false
  }
}

async function closeCurrentExam() {
  if (!editingExam.value) {
    return
  }
  closing.value = true
  try {
    const closed = await closeExam(editingExam.value.id)
    await fillEditor(await fetchAdminExamDetail(closed.id))
    ElMessage.success('考试已关闭')
    await loadExams()
  } finally {
    closing.value = false
  }
}

async function copyCurrentExam(exam: Exam) {
  const copied = await copyExam(exam.id)
  ElMessage.success('试卷已复制')
  await loadExams()
  await openEditEditor(copied)
}

async function downloadCurrentExam(exam: Exam) {
  const blob = await downloadExamPaper(exam.id)
  downloadBlob(blob, `${exam.title}.xlsx`)
}

async function revokeCurrentExam(exam: Exam) {
  await ElMessageBox.confirm(`确认撤销发布“${exam.title}”？`, '撤销发布', {
    type: 'warning',
    confirmButtonText: '撤销发布',
    cancelButtonText: '取消',
  })
  await revokeExam(exam.id)
  ElMessage.success('考试已撤销发布')
  await loadExams()
}

async function deleteCurrentExam(exam: Exam) {
  await ElMessageBox.confirm(`确认删除考试“${exam.title}”？没有作答或成绩的考试会连同草稿和发布快照一起删除。`, '删除考试', {
    type: 'warning',
    confirmButtonText: '删除考试',
    cancelButtonText: '取消',
  })
  await deleteExam(exam.id)
  ElMessage.success('考试已删除')
  if (editingExam.value?.id === exam.id) {
    editorVisible.value = false
    editingExam.value = null
  }
  await loadExams()
}

async function openResults(exam: Exam) {
  selectedResultExam.value = exam
  resultsVisible.value = true
  resultsLoading.value = true
  try {
    examResults.value = await fetchAdminResults({ examId: exam.id })
  } finally {
    resultsLoading.value = false
  }
}

function openResultDetail(resultId: number) {
  void router.push({ name: 'admin-result-detail', params: { resultId } })
}

async function buildPayload(): Promise<ExamPayload | null> {
  for (const rule of ruleset.value) {
    await loadBankQuestions(rule.bankId)
  }
  const payloadRules = toExamRulePayloads(ruleset.value)
  if (paperQuestions.value.length === 0 && payloadRules.length > 0) {
    await generatePaperQuestions()
  }
  if (paperQuestions.value.length === 0) {
    ElMessage.error('请先按规则生成题目明细或手工加入试题')
    return null
  }
  if (form.qualifyScore > totalScore.value) {
    ElMessage.error('及格分不能超过试卷总分')
    return null
  }
  if (form.openType === 'DEPARTMENT' && form.departmentIds.length === 0) {
    ElMessage.error('部门开放必须选择部门')
    return null
  }
  if (form.timeLimit) {
    if (!timeRange.value?.[0] || !timeRange.value?.[1]) {
      ElMessage.error('请选择开放时间')
      return null
    }
    form.startTime = timeRange.value[0]
    form.endTime = timeRange.value[1]
  } else {
    form.startTime = '2026-01-01T00:00:00'
    form.endTime = '2099-12-31T23:59:59'
  }
  form.attemptLimit = attemptLimitMode.value === 'LIMITED' ? Math.max(1, Number(limitedAttemptCount.value || 1)) : null
  return {
    title: form.title,
    description: form.description,
    qualifyScore: form.qualifyScore,
    startTime: form.startTime,
    endTime: form.endTime,
    durationMinutes: form.durationMinutes,
    timeLimit: form.timeLimit,
    attemptLimit: form.attemptLimit,
    displayMode: form.displayMode,
    questionOrderMode: form.questionOrderMode,
    openType: form.openType,
    departmentIds: [...form.departmentIds],
    rules: payloadRules,
    paperQuestions: toExamPaperQuestionPayloads(paperQuestions.value),
  }
}

function statusText(status: Exam['status']) {
  return status === 'PUBLISHED' ? '已发布' : status === 'CLOSED' ? '已关闭' : '草稿'
}

function statusType(status: Exam['status']) {
  return status === 'PUBLISHED' ? 'success' : status === 'CLOSED' ? 'info' : 'warning'
}

function displayModeText(displayMode: Exam['displayMode']) {
  return displayMode === 'ALL' ? '整卷一页' : '逐题显示'
}

function questionOrderText(mode: Exam['questionOrderMode']) {
  return mode === 'RANDOM' ? '随机顺序' : '固定顺序'
}

</script>
