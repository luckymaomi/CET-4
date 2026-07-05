<template>
  <el-dialog v-model="visible" :title="editingExam ? '编辑考试' : '新建考试'" width="min(1180px, 96vw)" class="exam-publish-dialog">
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
        <h2>试卷模式</h2>
        <el-segmented v-model="form.examMode" :options="examModeOptions" />
      </section>

      <ExamPaperBuilderSection
        v-if="form.examMode === 'STRUCTURED'"
        v-model:ruleset="ruleset"
        v-model:paper-questions="paperQuestions"
        :banks="banks"
        :bank-questions="bankQuestions"
        :picker="picker"
        :picker-questions="pickerQuestions"
        :picker-loading="pickerLoading"
        @add-rule="$emit('add-rule')"
        @remove-rule="$emit('remove-rule', $event)"
        @rule-bank-change="$emit('rule-bank-change', $event)"
        @mark-paper-stale="$emit('mark-paper-stale')"
        @generate-paper="$emit('generate-paper')"
        @preview-paper="$emit('preview-paper')"
        @load-picker-questions="$emit('load-picker-questions')"
        @add-manual-question="$emit('add-manual-question', $event)"
        @sort-paper="$emit('sort-paper')"
        @move-paper-question="(index, offset) => $emit('move-paper-question', index, offset)"
        @remove-paper-question="$emit('remove-paper-question', $event)"
      />

      <section v-else class="publish-section answer-sheet-editor">
        <div class="section-title-row">
          <h2>试卷材料</h2>
          <el-button @click="addMaterial">添加材料</el-button>
        </div>
        <el-table :data="materials" border>
          <el-table-column label="标题" min-width="150">
            <template #default="{ row }">
              <el-input v-model.trim="row.title" placeholder="如 听力材料" />
            </template>
          </el-table-column>
          <el-table-column label="地址" min-width="260">
            <template #default="{ row }">
              <el-input v-model.trim="row.fileUrl" placeholder="/local-assets/..." />
            </template>
          </el-table-column>
          <el-table-column label="类型" width="120">
            <template #default="{ row }">
              <el-select v-model="row.mediaType">
                <el-option label="PDF/文件" value="FILE" />
                <el-option label="图片" value="IMAGE" />
                <el-option label="音频" value="AUDIO" />
                <el-option label="视频" value="VIDEO" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ $index }">
              <el-button link type="danger" @click="materials.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="section-title-row answer-card-title">
          <h2>答题卡</h2>
          <el-button @click="addAnswerCardItem">添加题号</el-button>
        </div>
        <el-table :data="answerCardItems" border>
          <el-table-column label="题号" width="90">
            <template #default="{ row }">
              <el-input-number v-model="row.questionNo" :min="1" :controls="false" />
            </template>
          </el-table-column>
          <el-table-column label="类型" width="130">
            <template #default="{ row }">
              <el-select v-model="row.answerType" @change="normalizeCardItem(row)">
                <el-option label="单选" value="SINGLE_CHOICE" />
                <el-option label="多选" value="MULTIPLE_CHOICE" />
                <el-option label="写作" value="WRITING" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="选项" min-width="160">
            <template #default="{ row }">
              <el-input :model-value="row.optionLabels.join(',')" :disabled="row.answerType === 'WRITING'" @input="row.optionLabels = labelsFromText(String($event))" />
            </template>
          </el-table-column>
          <el-table-column label="答案" min-width="160">
            <template #default="{ row }">
              <el-input :model-value="row.correctLabels.join(',')" :disabled="row.answerType === 'WRITING'" @input="row.correctLabels = labelsFromText(String($event))" />
            </template>
          </el-table-column>
          <el-table-column label="分值" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.score" :min="0" :step="0.5" :controls="false" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ $index }">
              <el-button link type="danger" @click="answerCardItems.splice($index, 1)">删除</el-button>
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
          <el-button @click="visible = false">取消</el-button>
          <el-button v-if="editingExam && currentStatus !== 'CLOSED'" :loading="closing" @click="$emit('close-exam')">关闭考试</el-button>
          <el-button type="primary" plain :loading="saving" @click="$emit('save-draft')">保存草稿</el-button>
          <el-button type="primary" :disabled="!editingExam" :loading="publishing" @click="$emit('publish-exam')">发布考试</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

import type { Department } from '@/api/admin'
import type { Exam, ExamPayload, Question, QuestionBank } from '@/api/exam-business'
import type { ExamPaperQuestionForm, ExamRuleForm } from '@/utils/admin-exam-editor'
import ExamPaperBuilderSection from './ExamPaperBuilderSection.vue'

const visible = defineModel<boolean>('visible', { required: true })
const ruleset = defineModel<ExamRuleForm[]>('ruleset', { required: true })
const paperQuestions = defineModel<ExamPaperQuestionForm[]>('paperQuestions', { required: true })
const materials = defineModel<ExamPayload['materials']>('materials', { required: true })
const answerCardItems = defineModel<ExamPayload['answerCardItems']>('answerCardItems', { required: true })
const timeRange = defineModel<[string, string]>('timeRange', { required: true })
const attemptLimitMode = defineModel<'UNLIMITED' | 'LIMITED'>('attemptLimitMode', { required: true })
const limitedAttemptCount = defineModel<number | null>('limitedAttemptCount', { required: true })

defineProps<{
  editingExam: Exam | null
  currentStatus: Exam['status']
  form: ExamPayload
  formRules: FormRules<ExamPayload>
  totalScore: number
  totalQuestionCount: number
  banks: QuestionBank[]
  bankQuestions: Record<number, Question[]>
  picker: { bankId: number | null; keyword: string }
  pickerQuestions: Question[]
  pickerLoading: boolean
  departments: Department[]
  saving: boolean
  publishing: boolean
  closing: boolean
}>()

defineEmits<{
  'add-rule': []
  'remove-rule': [index: number]
  'rule-bank-change': [bankId: number | null]
  'mark-paper-stale': []
  'generate-paper': []
  'preview-paper': []
  'load-picker-questions': []
  'add-manual-question': [question: Question]
  'sort-paper': []
  'move-paper-question': [index: number, offset: number]
  'remove-paper-question': [index: number]
  'close-exam': []
  'save-draft': []
  'publish-exam': []
}>()

const formRef = ref<FormInstance>()

const displayModeOptions = [
  { label: '逐题显示', value: 'PAGED' },
  { label: '整卷一页', value: 'ALL' },
]

const examModeOptions = [
  { label: '结构化试卷', value: 'STRUCTURED' },
  { label: '答题卡试卷', value: 'ANSWER_SHEET' },
]

const questionOrderOptions = [
  { label: '固定顺序', value: 'FIXED' },
  { label: '随机顺序', value: 'RANDOM' },
]

function addMaterial() {
  materials.value.push({
    title: '试卷材料',
    description: '',
    fileName: 'paper.pdf',
    fileUrl: '',
    mediaType: 'FILE',
    sortOrder: (materials.value.length + 1) * 10,
  })
}

function addAnswerCardItem() {
  answerCardItems.value.push({
    questionNo: answerCardItems.value.length + 1,
    answerType: 'SINGLE_CHOICE',
    optionLabels: ['A', 'B', 'C', 'D'],
    correctLabels: ['A'],
    score: 5,
    sortOrder: (answerCardItems.value.length + 1) * 10,
  })
}

function normalizeCardItem(row: ExamPayload['answerCardItems'][number]) {
  if (row.answerType === 'WRITING') {
    row.optionLabels = []
    row.correctLabels = []
    return
  }
  if (row.optionLabels.length < 2) {
    row.optionLabels = ['A', 'B', 'C', 'D']
  }
  if (row.correctLabels.length === 0) {
    row.correctLabels = ['A']
  }
}

function labelsFromText(value: string) {
  return value
    .split(/[,，\s]+/)
    .map((label) => label.trim().toUpperCase())
    .filter(Boolean)
}

function statusText(status: Exam['status']) {
  return status === 'PUBLISHED' ? '已发布' : status === 'CLOSED' ? '已关闭' : '草稿'
}

defineExpose({
  validate: () => formRef.value?.validate(),
})
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

.publish-section h2 {
  margin: 0;
  font-size: 16px;
  letter-spacing: 0;
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.answer-sheet-editor {
  display: grid;
  gap: 14px;
}

.answer-card-title {
  margin-top: 6px;
}

.publish-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.publish-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
}

.publish-form-grid :deep(.el-input-number) {
  width: 100%;
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
  .publish-form-grid {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: auto;
  }
}
</style>
