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
      <el-table-column fixed="right" label="操作" width="310">
        <template #default="{ row }: { row: Exam }">
          <el-button link type="primary" @click="openEditEditor(row)">编辑</el-button>
          <el-button link type="primary" @click="openResults(row)">成绩</el-button>
          <el-button link type="primary" @click="copyCurrentExam(row)">复制</el-button>
          <el-button link type="primary" @click="downloadCurrentExam(row)">下载</el-button>
          <el-button v-if="row.status === 'PUBLISHED'" link type="warning" @click="revokeCurrentExam(row)">撤销发布</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-row">
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="loadExams" />
    </div>

    <el-dialog v-model="editorVisible" :title="editingExam ? '编辑考试' : '新建考试'" width="min(1180px, 96vw)" class="exam-publish-dialog">
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top" class="exam-publish">
        <header class="publish-summary">
          <div>
            <span>试卷总分</span>
            <strong>{{ totalScore }} 分</strong>
          </div>
          <div>
            <span>题目数量</span>
            <strong>{{ totalQuestionCount }} 题</strong>
          </div>
          <div>
            <span>开放范围</span>
            <strong>{{ form.openType === 'PUBLIC' ? '公开' : '部门' }}</strong>
          </div>
          <div>
            <span>当前状态</span>
            <strong>{{ statusText(currentStatus) }}</strong>
          </div>
        </header>

        <section class="publish-section">
          <div class="publish-section__head">
            <h2>组卷信息</h2>
            <el-button :icon="Plus" @click="addRule">添加题库</el-button>
          </div>
          <el-empty v-if="ruleset.length === 0" description="请添加题库并配置题量和分值" />
          <div v-else class="rule-list">
            <article v-for="(rule, index) in ruleset" :key="rule.rowId" class="rule-item">
              <el-form-item label="题库">
                <el-select v-model="rule.bankId" filterable placeholder="选择题库" class="form-control" @change="onRuleBankChange(rule.bankId)">
                  <el-option v-for="bank in availableBanks(rule)" :key="bank.id" :label="bank.name" :value="bank.id" />
                </el-select>
              </el-form-item>
              <div class="rule-fields">
                <el-form-item :label="`单选题（可用 ${bankStats(rule.bankId).single}）`">
                  <el-input-number v-model="rule.singleCount" :min="0" :max="bankStats(rule.bankId).single" :controls="false" @change="markPaperStale" />
                </el-form-item>
                <el-form-item label="单选分数">
                  <el-input-number v-model="rule.singleScore" :min="0" :step="0.5" :controls="false" @change="markPaperStale" />
                </el-form-item>
                <el-form-item :label="`多选题（可用 ${bankStats(rule.bankId).multiple}）`">
                  <el-input-number v-model="rule.multipleCount" :min="0" :max="bankStats(rule.bankId).multiple" :controls="false" @change="markPaperStale" />
                </el-form-item>
                <el-form-item label="多选分数">
                  <el-input-number v-model="rule.multipleScore" :min="0" :step="0.5" :controls="false" @change="markPaperStale" />
                </el-form-item>
              </div>
              <div class="rule-item__footer">
                <span>本题库小计：{{ ruleScore(rule) }} 分</span>
                <el-button link type="danger" @click="removeRule(index)">删除</el-button>
              </div>
            </article>
          </div>
        </section>

        <section class="publish-section">
          <div class="publish-section__head">
            <h2>题目明细</h2>
            <div class="header-actions">
              <el-button @click="generatePaperQuestions">按规则生成</el-button>
              <el-button :disabled="ruleset.length === 0" @click="generatePaperQuestions">更新试卷</el-button>
              <el-button :disabled="paperQuestions.length === 0" @click="previewVisible = true">预览试卷</el-button>
            </div>
          </div>

          <div class="manual-picker">
            <div class="manual-picker__toolbar">
              <el-select v-model="picker.bankId" filterable placeholder="选择题库" @change="loadPickerQuestions">
                <el-option v-for="bank in banks" :key="bank.id" :label="`${bank.name}（${bank.questionCount}题）`" :value="bank.id" />
              </el-select>
              <el-input v-model.trim="picker.keyword" clearable placeholder="搜索题干" @keyup.enter="loadPickerQuestions" />
              <el-button :icon="Search" @click="loadPickerQuestions">加载试题</el-button>
            </div>
            <el-table v-if="pickerQuestions.length" v-loading="pickerLoading" :data="pickerQuestions" border class="picker-table">
              <el-table-column prop="stem" label="可选试题" min-width="260" show-overflow-tooltip />
              <el-table-column label="题型" width="100">
                <template #default="{ row }: { row: Question }">
                  <el-tag effect="plain">{{ questionTypeText(row.type) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="题库" width="160" prop="bankName" />
              <el-table-column label="操作" width="110">
                <template #default="{ row }: { row: Question }">
                  <el-button link type="primary" :disabled="hasPaperQuestion(row.id)" @click="addManualQuestion(row)">
                    {{ hasPaperQuestion(row.id) ? '已加入' : '加入试卷' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <el-empty v-if="paperQuestions.length === 0" description="保存或发布前会按组卷规则生成题目明细" />
          <el-table v-else :data="paperQuestions" border class="paper-table">
            <el-table-column label="顺序" width="110">
              <template #default="{ row }: { row: ExamPaperQuestionForm }">
                <el-input-number v-model="row.sortOrder" :min="1" :step="1" step-strictly :controls="false" @change="sortPaperQuestions" />
              </template>
            </el-table-column>
            <el-table-column label="题型" width="100">
              <template #default="{ row }: { row: ExamPaperQuestionForm }">
                <el-tag effect="plain">{{ questionTypeText(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="题干" min-width="260">
              <template #default="{ row }: { row: ExamPaperQuestionForm }">
                <span class="paper-stem">{{ row.stem }}</span>
              </template>
            </el-table-column>
            <el-table-column label="题库" width="160">
              <template #default="{ row }: { row: ExamPaperQuestionForm }">
                {{ row.bankName }}
              </template>
            </el-table-column>
            <el-table-column label="分值" width="120">
              <template #default="{ row }: { row: ExamPaperQuestionForm }">
                <el-input-number v-model="row.score" :min="0.5" :step="0.5" :controls="false" />
              </template>
            </el-table-column>
            <el-table-column label="排序" width="140">
              <template #default="{ $index }: { $index: number }">
                <el-button link :disabled="$index === 0" @click="movePaperQuestion($index, -1)">上移</el-button>
                <el-button link :disabled="$index === paperQuestions.length - 1" @click="movePaperQuestion($index, 1)">下移</el-button>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="{ $index }: { $index: number }">
                <el-button link type="danger" @click="removePaperQuestion($index)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="publish-section">
          <h2>考试配置</h2>
          <div class="publish-form-grid">
            <el-form-item label="考试名称" prop="title" class="span-2">
              <el-input v-model.trim="form.title" maxlength="128" />
            </el-form-item>
            <el-form-item label="考试描述" class="span-2">
              <el-input v-model.trim="form.description" type="textarea" :rows="3" maxlength="500" />
            </el-form-item>
            <el-form-item label="及格分" prop="qualifyScore">
              <el-input-number v-model="form.qualifyScore" :min="0" :max="Math.max(totalScore, 0)" :step="0.5" />
            </el-form-item>
            <el-form-item label="考试时长" prop="durationMinutes">
              <el-input-number v-model="form.durationMinutes" :min="1" :step="5" />
            </el-form-item>
            <el-form-item label="可考次数">
              <div class="inline-control">
                <el-radio-group v-model="attemptLimitMode">
                  <el-radio-button value="UNLIMITED">无限次</el-radio-button>
                  <el-radio-button value="LIMITED">限定次数</el-radio-button>
                </el-radio-group>
                <el-input-number v-if="attemptLimitMode === 'LIMITED'" v-model="limitedAttemptCount" :min="1" :step="1" step-strictly />
              </div>
            </el-form-item>
            <el-form-item label="题目显示" prop="displayMode">
              <el-segmented v-model="form.displayMode" :options="displayModeOptions" />
            </el-form-item>
            <el-form-item label="题目顺序" prop="questionOrderMode">
              <el-segmented v-model="form.questionOrderMode" :options="questionOrderOptions" />
            </el-form-item>
            <el-form-item label="考试日期">
              <el-switch v-model="form.timeLimit" active-text="限时开放" inactive-text="不限日期" />
            </el-form-item>
            <el-form-item v-if="form.timeLimit" label="开放时间" class="span-2" required>
              <el-date-picker
                v-model="timeRange"
                type="datetimerange"
                value-format="YYYY-MM-DDTHH:mm:ss"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                class="form-control"
              />
            </el-form-item>
          </div>
        </section>

        <section class="publish-section">
          <h2>权限配置</h2>
          <div class="publish-form-grid">
            <el-form-item label="开放范围" prop="openType">
              <el-radio-group v-model="form.openType">
                <el-radio-button value="PUBLIC">公开考试</el-radio-button>
                <el-radio-button value="DEPARTMENT">部门开放</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="form.openType === 'DEPARTMENT'" label="开放部门">
              <el-tree-select
                v-model="form.departmentIds"
                :data="departments"
                :props="{ label: 'name', value: 'id', children: 'children' }"
                multiple
                check-strictly
                collapse-tags
                collapse-tags-tooltip
                class="form-control"
                placeholder="选择可参加考试的部门"
              />
            </el-form-item>
          </div>
        </section>
      </el-form>

      <template #footer>
        <div class="publish-footer">
          <span>保存草稿会持久化当前组卷规则；发布考试会生成本次发布快照。</span>
          <div class="footer-actions">
            <el-button @click="editorVisible = false">取消</el-button>
            <el-button v-if="editingExam && currentStatus !== 'CLOSED'" :loading="closing" @click="closeCurrentExam">关闭考试</el-button>
            <el-button type="primary" plain :loading="saving" @click="saveDraft">保存草稿</el-button>
            <el-button type="primary" :disabled="!editingExam" :loading="publishing" @click="publishCurrentExam">发布考试</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="试卷预览" width="min(920px, 94vw)">
      <div class="paper-preview">
        <article v-for="(question, index) in paperQuestions" :key="question.questionId" class="preview-question">
          <header>
            <strong>{{ index + 1 }}. {{ question.stem }}</strong>
            <el-tag effect="plain">{{ questionTypeText(question.type) }} · {{ question.score }} 分</el-tag>
          </header>
          <span class="muted-text">{{ question.bankName }}</span>
        </article>
      </div>
    </el-dialog>

    <el-drawer v-model="resultsVisible" :title="`${selectedResultExam?.title || '考试'} - 成绩`" size="min(920px, 92vw)">
      <div class="result-drawer">
        <header class="result-summary">
          <div>
            <span>考试人数</span>
            <strong>{{ examResults.length }}</strong>
          </div>
          <div>
            <span>通过人数</span>
            <strong>{{ passedResultCount }}</strong>
          </div>
          <div>
            <span>平均分</span>
            <strong>{{ averageResultScore }}</strong>
          </div>
        </header>
        <el-table v-loading="resultsLoading" :data="examResults" border class="data-table">
          <el-table-column prop="userName" label="姓名" min-width="120" />
          <el-table-column prop="username" label="账号" min-width="130" />
          <el-table-column label="部门" min-width="160">
            <template #default="{ row }: { row: ExamResult }">{{ row.departmentName || '未分配' }}</template>
          </el-table-column>
          <el-table-column label="成绩" width="120">
            <template #default="{ row }: { row: ExamResult }">{{ row.obtainedScore }} / {{ row.totalScore }}</template>
          </el-table-column>
          <el-table-column label="考试结果" width="110">
            <template #default="{ row }: { row: ExamResult }">
              <el-tag :type="row.passed ? 'success' : 'danger'" effect="plain">{{ row.passed ? '通过' : '未通过' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="180">
            <template #default="{ row }: { row: ExamResult }">{{ formatDateTime(row.submittedAt) }}</template>
          </el-table-column>
          <el-table-column fixed="right" label="操作" width="110">
            <template #default="{ row }: { row: ExamResult }">
              <el-button link type="primary" @click="router.push({ name: 'admin-result-detail', params: { resultId: row.id } })">
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

import { fetchDepartments, type Department } from '@/api/admin'
import {
  closeExam,
  copyExam,
  createExam,
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
  type ExamPaperQuestion,
  type ExamPayload,
  type ExamResult,
  type Question,
  type QuestionBank,
} from '@/api/exam-business'
import { downloadBlob } from '@/utils/download'
import { formatDateTime } from '@/utils/datetime'

interface ExamRuleForm {
  rowId: number
  bankId: number | null
  singleCount: number
  singleScore: number
  multipleCount: number
  multipleScore: number
}

interface ExamPaperQuestionForm {
  questionId: number
  bankId: number
  bankName: string
  type: Question['type']
  stem: string
  score: number
  sortOrder: number
}

const displayModeOptions = [
  { label: '逐题显示', value: 'PAGED' },
  { label: '整卷一页', value: 'ALL' },
]

const questionOrderOptions = [
  { label: '固定顺序', value: 'FIXED' },
  { label: '随机顺序', value: 'RANDOM' },
]

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
const formRef = ref<FormInstance>()
const timeRange = ref<[string, string]>(['2026-01-01T00:00:00', '2026-12-31T23:59:59'])
const attemptLimitMode = ref<'UNLIMITED' | 'LIMITED'>('UNLIMITED')
const limitedAttemptCount = ref(1)
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

const totalScore = computed(() => {
  if (paperQuestions.value.length > 0) {
    return paperQuestions.value.reduce((sum, question) => sum + question.score, 0)
  }
  return ruleset.value.reduce((sum, rule) => sum + ruleScore(rule), 0)
})
const totalQuestionCount = computed(() => {
  if (paperQuestions.value.length > 0) {
    return paperQuestions.value.length
  }
  return ruleset.value.reduce((sum, rule) => sum + rule.singleCount + rule.multipleCount, 0)
})
const passedResultCount = computed(() => examResults.value.filter((result) => result.passed).length)
const averageResultScore = computed(() => {
  if (examResults.value.length === 0) {
    return '-'
  }
  const value = examResults.value.reduce((sum, result) => sum + Number(result.obtainedScore), 0) / examResults.value.length
  return value.toFixed(1)
})

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
  ruleset.value = exam.rules.map((rule) => ({
    rowId: Date.now() + rule.id,
    bankId: rule.bankId,
    singleCount: rule.singleCount,
    singleScore: rule.singleScore,
    multipleCount: rule.multipleCount,
    multipleScore: rule.multipleScore,
  }))
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
  ruleset.value.push({
    rowId: Date.now() + ruleset.value.length,
    bankId: null,
    singleCount: 0,
    singleScore: 5,
    multipleCount: 0,
    multipleScore: 5,
  })
  markPaperStale()
}

function removeRule(index: number) {
  ruleset.value.splice(index, 1)
  markPaperStale()
}

function availableBanks(rule: ExamRuleForm) {
  const selected = new Set(ruleset.value.filter((item) => item.rowId !== rule.rowId).map((item) => item.bankId))
  return banks.value.filter((bank) => !selected.has(bank.id))
}

async function loadBankQuestions(bankId: number | null) {
  if (!bankId || bankQuestions.value[bankId]) {
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

function bankStats(bankId: number | null) {
  const questions = bankId ? bankQuestions.value[bankId] || [] : []
  return {
    single: questions.filter((question) => question.type === 'SINGLE_CHOICE').length,
    multiple: questions.filter((question) => question.type === 'MULTIPLE_CHOICE').length,
  }
}

function ruleScore(rule: ExamRuleForm) {
  return rule.singleCount * rule.singleScore + rule.multipleCount * rule.multipleScore
}

async function generatePaperQuestions() {
  for (const rule of ruleset.value) {
    await loadBankQuestions(rule.bankId)
  }
  const generated: ExamPaperQuestionForm[] = []
  let sortOrder = 10
  for (const rule of ruleset.value) {
    const questions = rule.bankId ? bankQuestions.value[rule.bankId] || [] : []
    const singles = questions
      .filter((question) => question.type === 'SINGLE_CHOICE')
      .sort((left, right) => left.id - right.id)
      .slice(0, rule.singleCount)
    const multiples = questions
      .filter((question) => question.type === 'MULTIPLE_CHOICE')
      .sort((left, right) => left.id - right.id)
      .slice(0, rule.multipleCount)
    for (const question of singles) {
      generated.push(toGeneratedPaperQuestion(question, rule.singleScore, sortOrder))
      sortOrder += 10
    }
    for (const question of multiples) {
      generated.push(toGeneratedPaperQuestion(question, rule.multipleScore, sortOrder))
      sortOrder += 10
    }
  }
  paperQuestions.value = generated
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
  const matchedRule = ruleset.value.find((rule) => {
    if (!rule.bankId || rule.bankId !== picker.bankId) {
      return false
    }
    return type === 'SINGLE_CHOICE' ? rule.singleScore > 0 : rule.multipleScore > 0
  })
  if (!matchedRule) {
    return 5
  }
  return type === 'SINGLE_CHOICE' ? matchedRule.singleScore : matchedRule.multipleScore
}

function toGeneratedPaperQuestion(question: Question, score: number, sortOrder: number): ExamPaperQuestionForm {
  return {
    questionId: question.id,
    bankId: question.bankId,
    bankName: question.bankName,
    type: question.type,
    stem: question.stem,
    score,
    sortOrder,
  }
}

function toPaperQuestionForm(question: ExamPaperQuestion): ExamPaperQuestionForm {
  return {
    questionId: question.questionId,
    bankId: question.bankId,
    bankName: question.bankName,
    type: question.type,
    stem: question.stem,
    score: question.score,
    sortOrder: question.sortOrder,
  }
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

function normalizePaperSort(rows: ExamPaperQuestionForm[]) {
  return rows.map((row, index) => ({
    ...row,
    sortOrder: (index + 1) * 10,
  }))
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

async function buildPayload(): Promise<ExamPayload | null> {
  for (const rule of ruleset.value) {
    await loadBankQuestions(rule.bankId)
  }
  const payloadRules = ruleset.value
    .filter((rule) => rule.bankId)
    .map((rule) => ({
      bankId: Number(rule.bankId),
      singleCount: rule.singleCount,
      singleScore: rule.singleCount === 0 ? 0 : rule.singleScore,
      multipleCount: rule.multipleCount,
      multipleScore: rule.multipleCount === 0 ? 0 : rule.multipleScore,
    }))
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
  form.attemptLimit = attemptLimitMode.value === 'LIMITED' ? limitedAttemptCount.value : null
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
    paperQuestions: paperQuestions.value.map((question) => ({
      questionId: question.questionId,
      score: question.score,
      sortOrder: question.sortOrder,
    })),
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

function questionTypeText(type: Question['type']) {
  return type === 'MULTIPLE_CHOICE' ? '多选题' : '单选题'
}
</script>

<style scoped>
.exam-publish {
  display: grid;
  gap: 18px;
}

.publish-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--ks-border);
  border-radius: var(--ks-radius);
  background: var(--ks-panel-muted);
}

.publish-summary div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.publish-summary span {
  color: var(--ks-text-muted);
  font-size: 13px;
}

.publish-summary strong {
  color: var(--ks-text);
  overflow-wrap: anywhere;
}

.publish-section {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.publish-section + .publish-section {
  padding-top: 18px;
  border-top: 1px solid var(--ks-border);
}

.publish-section h2 {
  margin: 0;
  font-size: 16px;
  letter-spacing: 0;
}

.publish-section__head,
.rule-item__footer,
.publish-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.rule-list {
  display: grid;
  gap: 12px;
}

.rule-item {
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--ks-border);
  border-radius: var(--ks-radius);
}

.rule-fields {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.rule-fields :deep(.el-input-number),
.publish-form-grid :deep(.el-input-number) {
  width: 100%;
}

.rule-item__footer {
  color: var(--ks-warning);
  font-weight: 700;
}

.paper-table :deep(.el-input-number) {
  width: 100%;
}

.manual-picker {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.manual-picker__toolbar {
  display: grid;
  grid-template-columns: minmax(180px, 260px) minmax(220px, 1fr) auto;
  gap: 10px;
  align-items: center;
  min-width: 0;
}

.manual-picker__toolbar :deep(.el-select) {
  width: 100%;
}

.picker-table {
  width: 100%;
}

.paper-stem {
  display: block;
  overflow-wrap: anywhere;
  line-height: 1.5;
}

.paper-preview {
  display: grid;
  gap: 12px;
  max-height: min(70vh, 680px);
  overflow: auto;
}

.result-drawer {
  display: grid;
  gap: 14px;
}

.result-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.result-summary div {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--ks-border);
  border-radius: var(--ks-radius);
  background: var(--ks-panel-muted);
}

.result-summary span {
  color: var(--ks-text-muted);
  font-size: 13px;
}

.result-summary strong {
  font-size: 18px;
}

.preview-question {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 12px 0;
  border-bottom: 1px solid var(--ks-border);
}

.preview-question header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.preview-question strong {
  min-width: 0;
  overflow-wrap: anywhere;
}

.publish-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
}

.span-2 {
  grid-column: 1 / -1;
}

.inline-control,
.footer-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  min-width: 0;
}

.publish-footer > span {
  color: var(--ks-text-muted);
  font-size: 13px;
}

@media (max-width: 900px) {
  .publish-summary,
  .rule-fields,
  .publish-form-grid,
  .manual-picker__toolbar {
    grid-template-columns: 1fr;
  }

  .result-summary {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: auto;
  }
}
</style>
