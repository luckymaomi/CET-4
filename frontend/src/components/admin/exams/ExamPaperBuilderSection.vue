<template>
  <section class="publish-section">
    <div class="publish-section__head">
      <h2>组卷信息</h2>
      <el-button :icon="Plus" @click="$emit('add-rule')">添加题库</el-button>
    </div>
    <el-empty v-if="ruleset.length === 0" description="请添加题库并配置题量和分值" />
    <div v-else class="rule-list">
      <article v-for="(rule, index) in ruleset" :key="rule.rowId" class="rule-item">
        <el-form-item label="题库">
          <el-select v-model="rule.bankId" filterable placeholder="选择题库" class="form-control" @change="$emit('rule-bank-change', rule.bankId)">
            <el-option v-for="bank in availableBanks(rule)" :key="bank.id" :label="bank.name" :value="bank.id" />
          </el-select>
        </el-form-item>
        <div class="rule-fields">
          <el-form-item :label="`单选题（可用 ${bankStats(rule.bankId).single}）`">
            <el-input-number v-model="rule.singleCount" :min="0" :max="bankStats(rule.bankId).single" :controls="false" @change="$emit('mark-paper-stale')" />
          </el-form-item>
          <el-form-item label="单选分数">
            <el-input-number v-model="rule.singleScore" :min="0" :step="0.5" :controls="false" @change="$emit('mark-paper-stale')" />
          </el-form-item>
          <el-form-item :label="`多选题（可用 ${bankStats(rule.bankId).multiple}）`">
            <el-input-number v-model="rule.multipleCount" :min="0" :max="bankStats(rule.bankId).multiple" :controls="false" @change="$emit('mark-paper-stale')" />
          </el-form-item>
          <el-form-item label="多选分数">
            <el-input-number v-model="rule.multipleScore" :min="0" :step="0.5" :controls="false" @change="$emit('mark-paper-stale')" />
          </el-form-item>
          <el-form-item :label="`写作题（可用 ${bankStats(rule.bankId).writing}）`">
            <el-input-number v-model="rule.writingCount" :min="0" :max="bankStats(rule.bankId).writing" :controls="false" @change="$emit('mark-paper-stale')" />
          </el-form-item>
          <el-form-item label="写作分数">
            <el-input-number v-model="rule.writingScore" :min="0" :step="0.5" :controls="false" @change="$emit('mark-paper-stale')" />
          </el-form-item>
        </div>
        <div class="rule-item__footer">
          <span>本题库小计：{{ ruleScore(rule) }} 分</span>
          <el-button link type="danger" @click="$emit('remove-rule', index)">删除</el-button>
        </div>
      </article>
    </div>
  </section>

  <section class="publish-section">
    <div class="publish-section__head">
      <h2>题目明细</h2>
      <div class="header-actions">
        <el-button @click="$emit('generate-paper')">按规则生成</el-button>
        <el-button :disabled="ruleset.length === 0" @click="$emit('generate-paper')">更新试卷</el-button>
        <el-button :disabled="paperQuestions.length === 0" @click="$emit('preview-paper')">预览试卷</el-button>
      </div>
    </div>

    <div class="manual-picker">
      <div class="manual-picker__toolbar">
        <el-select v-model="picker.bankId" filterable placeholder="选择题库" @change="$emit('load-picker-questions')">
          <el-option v-for="bank in banks" :key="bank.id" :label="`${bank.name}（${bank.questionCount}题）`" :value="bank.id" />
        </el-select>
        <el-input v-model.trim="picker.keyword" clearable placeholder="搜索题干" @keyup.enter="$emit('load-picker-questions')" />
        <el-button :icon="Search" @click="$emit('load-picker-questions')">加载试题</el-button>
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
            <el-button link type="primary" :disabled="hasPaperQuestion(row.id)" @click="$emit('add-manual-question', row)">
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
          <el-input-number v-model="row.sortOrder" :min="1" :step="1" step-strictly :controls="false" @change="$emit('sort-paper')" />
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
        <template #default="{ row }: { row: ExamPaperQuestionForm }">{{ row.bankName }}</template>
      </el-table-column>
      <el-table-column label="分值" width="120">
        <template #default="{ row }: { row: ExamPaperQuestionForm }">
          <el-input-number v-model="row.score" :min="0.5" :step="0.5" :controls="false" />
        </template>
      </el-table-column>
      <el-table-column label="排序" width="140">
        <template #default="{ $index }: { $index: number }">
          <el-button link :disabled="$index === 0" @click="$emit('move-paper-question', $index, -1)">上移</el-button>
          <el-button link :disabled="$index === paperQuestions.length - 1" @click="$emit('move-paper-question', $index, 1)">下移</el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="{ $index }: { $index: number }">
          <el-button link type="danger" @click="$emit('remove-paper-question', $index)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { Plus, Search } from '@element-plus/icons-vue'

import type { Question, QuestionBank } from '@/api/exam-business'
import {
  bankStats as calculateBankStats,
  ruleScore,
  type ExamPaperQuestionForm,
  type ExamRuleForm,
} from '@/utils/admin-exam-editor'
import { questionTypeText } from '@/utils/question-types'

const ruleset = defineModel<ExamRuleForm[]>('ruleset', { required: true })
const paperQuestions = defineModel<ExamPaperQuestionForm[]>('paperQuestions', { required: true })

const props = defineProps<{
  banks: QuestionBank[]
  bankQuestions: Record<number, Question[]>
  picker: { bankId: number | null; keyword: string }
  pickerQuestions: Question[]
  pickerLoading: boolean
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
}>()

function availableBanks(rule: ExamRuleForm) {
  const selected = new Set(ruleset.value.filter((item) => item.rowId !== rule.rowId).map((item) => item.bankId))
  return props.banks.filter((bank) => !selected.has(bank.id))
}

function bankStats(bankId: number | null) {
  return calculateBankStats(bankId ? props.bankQuestions[bankId] || [] : [])
}

function hasPaperQuestion(questionId: number) {
  return paperQuestions.value.some((question) => question.questionId === questionId)
}
</script>

<style scoped>
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
.rule-item__footer {
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

.rule-fields :deep(.el-input-number) {
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

@media (max-width: 900px) {
  .rule-fields,
  .manual-picker__toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
