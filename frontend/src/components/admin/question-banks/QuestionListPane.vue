<template>
  <main class="question-pane">
    <section class="selected-bank">
      <div class="entity-stack">
        <span class="muted-text">当前题库</span>
        <h2>{{ selectedBank?.name || selectedCategory?.name || '全部题库' }}</h2>
        <span class="muted-text">
          {{ selectedBank ? `${selectedBank.categoryName} · ${selectedBank.questionCount} 题` : selectedCategory ? '当前选中分类，可在该分类下新建题库' : '选择左侧题库后可维护该题库试题' }}
        </span>
      </div>
      <div class="header-actions">
        <el-button v-if="selectedBank" @click="$emit('edit-bank', selectedBank)">编辑题库</el-button>
        <el-button v-if="selectedCategory && !selectedBank" @click="$emit('edit-category', selectedCategory.id)">编辑分类</el-button>
        <el-button type="primary" :disabled="bankCount === 0" @click="$emit('create-question')">新建试题</el-button>
      </div>
    </section>

    <div class="toolbar">
      <el-input v-model.trim="query.keyword" clearable placeholder="搜索题干或题库" class="toolbar__search" @keyup.enter="$emit('search')" />
      <el-button :icon="Search" @click="$emit('search')">搜索</el-button>
      <el-button v-if="selectedBankId" @click="$emit('clear-selection')">查看全部题库</el-button>
    </div>

    <el-table v-loading="loading" :data="questions" class="data-table" border>
      <el-table-column prop="stem" label="题干" min-width="260" show-overflow-tooltip />
      <el-table-column prop="bankName" label="题库" width="160" />
      <el-table-column label="题型" width="110">
        <template #default="{ row }: { row: Question }">{{ questionTypeText(row.type) }}</template>
      </el-table-column>
      <el-table-column label="答案" min-width="140">
        <template #default="{ row }: { row: Question }">
          <span v-if="!questionTypeMeta(row.type).optionBased" class="muted-text">主观阅卷</span>
          <template v-else>
            <el-tag v-for="option in row.options.filter((item) => item.correct)" :key="option.id" class="answer-tag">
              {{ option.label }}
            </el-tag>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }: { row: Question }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="plain">
            {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="120">
        <template #default="{ row }: { row: Question }">
          <el-button link type="primary" @click="$emit('edit-question', row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-row">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="$emit('search')"
      />
    </div>
  </main>
</template>

<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'

import type { NamedCategory, Question, QuestionBank } from '@/api/exam-business'
import { questionTypeMeta, questionTypeText } from '@/utils/question-types'

defineProps<{
  selectedBank: QuestionBank | null
  selectedCategory: NamedCategory | null
  selectedBankId: number | null
  bankCount: number
  questions: Question[]
  loading: boolean
  total: number
  query: { page: number; size: number; keyword: string }
}>()

defineEmits<{
  search: []
  'clear-selection': []
  'edit-bank': [bank: QuestionBank]
  'edit-category': [categoryId: number]
  'create-question': []
  'edit-question': [question: Question]
}>()
</script>

<style scoped>
.question-pane {
  display: grid;
  gap: 14px;
  min-width: 0;
  padding: 18px;
  border: 1px solid var(--ks-border);
  border-radius: var(--ks-radius);
  background: var(--ks-panel);
}

.selected-bank {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.selected-bank h2 {
  margin: 0;
  font-size: 20px;
  letter-spacing: 0;
}

@media (max-width: 900px) {
  .selected-bank {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
