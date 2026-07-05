<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1>{{ pageTitle }}</h1>
      </div>
      <div class="header-actions">
        <el-button :icon="Download" @click="downloadTemplate">下载模板</el-button>
        <el-upload :show-file-list="false" accept=".xlsx" :before-upload="handleImport">
          <el-button :icon="Upload">导入试题</el-button>
        </el-upload>
        <el-button :icon="Plus" @click="openCreateBankDialog()">新建题库</el-button>
        <el-button type="primary" :icon="Plus" :disabled="banks.length === 0" @click="openCreateQuestionDialog">新建试题</el-button>
      </div>
    </header>

    <el-alert v-if="importResult" :type="importResult.failureCount ? 'warning' : 'success'" show-icon :closable="false">
      <template #title>
        导入完成：成功 {{ importResult.successCount }} 条，失败 {{ importResult.failureCount }} 条
      </template>
      <ul v-if="importResult.errors.length" class="import-errors">
        <li v-for="error in importResult.errors" :key="error">{{ error }}</li>
      </ul>
    </el-alert>

    <div class="question-workbench">
      <QuestionBankTreePane
        ref="bankTreeRef"
        v-model:keyword="bankKeyword"
        :tree="bankTree"
        :loading="bankLoading"
        :bank-count="banks.length"
        :question-total="bankQuestionTotal"
        @search="loadBanks"
        @select-node="selectBankNode"
        @create-category="openCreateCategoryDialog"
        @create-bank="openCreateBankDialog"
        @edit-category="openEditCategoryDialog"
        @delete-category="deleteCategory"
      />

      <QuestionListPane
        :selected-bank="selectedBank"
        :selected-category="selectedCategory"
        :selected-bank-id="selectedBankId"
        :bank-count="banks.length"
        :questions="questions"
        :loading="loading"
        :total="total"
        :query="query"
        @search="loadQuestions"
        @clear-selection="clearSelectedBank"
        @edit-bank="openEditBankDialog"
        @edit-category="openEditCategoryDialog"
        @create-question="openCreateQuestionDialog"
        @edit-question="openEditQuestionDialog"
      />
    </div>

    <QuestionCategoryDialog
      ref="categoryFormRef"
      v-model:visible="categoryDialogVisible"
      :editing="Boolean(editingCategory)"
      :form="categoryForm"
      :rules="categoryRules"
      :saving="categorySaving"
      @submit="submitCategory"
    />

    <QuestionBankDialog
      ref="bankFormRef"
      v-model:visible="bankDialogVisible"
      :editing="Boolean(editingBank)"
      :form="bankForm"
      :rules="bankRules"
      :categories="categories"
      :saving="bankSaving"
      @submit="submitBank"
    />

    <QuestionEditorDialog
      ref="questionFormRef"
      v-model:visible="questionDialogVisible"
      v-model:attachment-url="attachmentUrl"
      v-model:attachment-media-type="attachmentMediaType"
      :editing="Boolean(editingQuestion)"
      :form="questionForm"
      :rules="questionRules"
      :banks="banks"
      :saving="saving"
      :uploading-attachment="uploadingAttachment"
      @normalize-options="normalizeQuestionOptions"
      @add-option="addOption"
      @remove-option="removeOption"
      @add-url-attachment="addUrlAttachment"
      @remove-attachment="removeAttachment"
      @move-attachment="moveAttachment"
      @upload="handleAttachmentUpload"
      @submit="submitQuestion"
    />
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Download, Plus, Upload } from '@element-plus/icons-vue'

import QuestionBankDialog from '@/components/admin/question-banks/QuestionBankDialog.vue'
import QuestionListPane from '@/components/admin/question-banks/QuestionListPane.vue'
import QuestionBankTreePane from '@/components/admin/question-banks/QuestionBankTreePane.vue'
import QuestionCategoryDialog from '@/components/admin/question-banks/QuestionCategoryDialog.vue'
import QuestionEditorDialog from '@/components/admin/question-banks/QuestionEditorDialog.vue'
import { useQuestionBankCatalog } from '@/composables/useQuestionBankCatalog'
import { useQuestionEditor } from '@/composables/useQuestionEditor'

const pageTitle = '题库管理'

const questionFormRef = ref<{ validate: () => Promise<boolean> | undefined }>()
const bankFormRef = ref<{ validate: () => Promise<boolean> | undefined }>()
const categoryFormRef = ref<{ validate: () => Promise<boolean> | undefined }>()

async function refreshFirstQuestionPage() {
  await questionEditor.loadFirstPage()
}

const catalog = useQuestionBankCatalog(refreshFirstQuestionPage)
const questionEditor = useQuestionEditor(
  catalog.selectedBankId,
  catalog.selectedCategoryId,
  () => catalog.banks.value[0]?.id || 1,
  (categoryId) => catalog.banks.value.find((bank) => bank.categoryId === categoryId)?.id,
  async (bankId) => {
    await catalog.loadBanks(bankId || catalog.selectedBankId.value)
    await questionEditor.loadQuestions()
  },
)

const {
  categories,
  banks,
  selectedBankId,
  selectedBank,
  selectedCategory,
  bankQuestionTotal,
  bankTree,
  bankLoading,
  bankSaving,
  categorySaving,
  categoryDialogVisible,
  bankDialogVisible,
  editingCategory,
  editingBank,
  bankTreeRef,
  bankKeyword,
  categoryForm,
  bankForm,
  bankRules,
  categoryRules,
  loadCategories,
  loadBanks,
  selectBankNode,
  clearSelectedBank,
  openCreateCategoryDialog,
  openEditCategoryDialog,
  deleteCategory,
  openCreateBankDialog,
  openEditBankDialog,
} = catalog

const {
  questions,
  total,
  loading,
  saving,
  uploadingAttachment,
  questionDialogVisible,
  editingQuestion,
  importResult,
  attachmentUrl,
  attachmentMediaType,
  query,
  questionForm,
  questionRules,
  loadQuestions,
  openCreateQuestionDialog,
  openEditQuestionDialog,
  addOption,
  removeOption,
  normalizeQuestionOptions,
  downloadTemplate,
  handleImport,
  handleAttachmentUpload,
  addUrlAttachment,
  removeAttachment,
  moveAttachment,
} = questionEditor

onMounted(async () => {
  await Promise.all([loadCategories(), loadBanks()])
  await loadQuestions()
})

function submitCategory() {
  return catalog.submitCategory(() => categoryFormRef.value?.validate())
}

function submitBank() {
  return catalog.submitBank(() => bankFormRef.value?.validate())
}

function submitQuestion() {
  return questionEditor.submitQuestion(() => questionFormRef.value?.validate())
}
</script>

<style scoped>
.question-workbench {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
  min-width: 0;
}

.question-workbench :deep(.tree-pane),
.question-workbench :deep(.question-pane) {
  min-height: 356px;
}

.question-workbench :deep(.question-pane) {
  align-self: stretch;
}

.import-errors {
  margin: 8px 0 0;
  padding-left: 18px;
}

@media (max-width: 900px) {
  .question-workbench {
    grid-template-columns: 1fr;
  }
}
</style>
