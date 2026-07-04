<template>
  <el-drawer :model-value="visible" :title="`${exam?.title || '考试'} - 成绩`" size="min(920px, 92vw)" @update:model-value="$emit('update:visible', $event)">
    <div class="result-drawer">
      <header class="result-summary">
        <div>
          <span>考试人数</span>
          <strong>{{ results.length }}</strong>
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
      <el-table v-loading="loading" :data="results" border class="data-table">
        <el-table-column prop="userName" label="姓名" min-width="120" />
        <el-table-column prop="username" label="账号" min-width="130" />
        <el-table-column label="部门" min-width="160">
          <template #default="{ row }: { row: ExamResult }">{{ row.departmentName || '未分配' }}</template>
        </el-table-column>
        <el-table-column label="成绩" width="120">
          <template #default="{ row }: { row: ExamResult }">{{ row.obtainedScore }} / {{ row.totalScore }}</template>
        </el-table-column>
        <el-table-column label="阅卷状态" width="110">
          <template #default="{ row }: { row: ExamResult }">
            <el-tag :type="row.gradingStatus === 'FINAL' ? 'success' : 'warning'" effect="plain">
              {{ row.gradingStatus === 'FINAL' ? '已出分' : '待阅卷' }}
            </el-tag>
          </template>
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
            <el-button link type="primary" @click="$emit('open-detail', row.id)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { Exam, ExamResult } from '@/api/exam-business'
import { formatDateTime } from '@/utils/datetime'

const props = defineProps<{
  visible: boolean
  exam: Exam | null
  results: ExamResult[]
  loading: boolean
}>()

defineEmits<{
  'update:visible': [visible: boolean]
  'open-detail': [resultId: number]
}>()

const passedResultCount = computed(() => props.results.filter((result) => result.passed).length)
const averageResultScore = computed(() => {
  if (props.results.length === 0) {
    return '-'
  }
  const value = props.results.reduce((sum, result) => sum + Number(result.obtainedScore), 0) / props.results.length
  return value.toFixed(1)
})
</script>

<style scoped>
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

@media (max-width: 900px) {
  .result-summary {
    grid-template-columns: 1fr;
  }
}
</style>
