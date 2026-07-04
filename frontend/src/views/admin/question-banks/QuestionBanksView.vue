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
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormRules, type UploadRawFile } from 'element-plus'
import { Download, Plus, Upload } from '@element-plus/icons-vue'

import QuestionBankDialog from '@/components/admin/question-banks/QuestionBankDialog.vue'
import QuestionListPane from '@/components/admin/question-banks/QuestionListPane.vue'
import QuestionBankTreePane from '@/components/admin/question-banks/QuestionBankTreePane.vue'
import QuestionCategoryDialog from '@/components/admin/question-banks/QuestionCategoryDialog.vue'
import QuestionEditorDialog from '@/components/admin/question-banks/QuestionEditorDialog.vue'
import {
  createQuestion,
  createQuestionBank,
  createQuestionCategory,
  deleteQuestionCategory,
  downloadQuestionImportTemplate,
  fetchQuestionBanks,
  fetchQuestionCategories,
  fetchQuestions,
  importQuestions,
  updateQuestion,
  updateQuestionBank,
  updateQuestionCategory,
  uploadFile,
  type NamedCategory,
  type Question,
  type QuestionAttachmentPayload,
  type QuestionBank,
  type QuestionBankPayload,
  type QuestionCategoryPayload,
  type QuestionPayload,
} from '@/api/exam-business'
import type { ExcelImportResult } from '@/api/admin'
import { downloadBlob } from '@/utils/download'
import {
  buildBankTree,
  createQuestionPayload,
  inferMediaType,
  nextOptionLabel,
  normalizeOptionsForType,
  questionOptionError,
  questionToPayload,
  type BankTreeNode,
} from '@/utils/question-bank-editor'

const pageTitle = '题库管理'

const categories = ref<NamedCategory[]>([])
const banks = ref<QuestionBank[]>([])
const selectedCategoryId = ref<number | null>(null)
const selectedBankId = ref<number | null>(null)
const questions = ref<Question[]>([])
const total = ref(0)
const loading = ref(false)
const bankLoading = ref(false)
const saving = ref(false)
const bankSaving = ref(false)
const categorySaving = ref(false)
const uploadingAttachment = ref(false)
const categoryDialogVisible = ref(false)
const bankDialogVisible = ref(false)
const questionDialogVisible = ref(false)
const editingCategory = ref<NamedCategory | null>(null)
const editingBank = ref<QuestionBank | null>(null)
const editingQuestion = ref<Question | null>(null)
const importResult = ref<ExcelImportResult | null>(null)
const attachmentUrl = ref('')
const attachmentMediaType = ref<QuestionAttachmentPayload['mediaType']>('FILE')
const questionFormRef = ref<{ validate: () => Promise<boolean> | undefined }>()
const bankFormRef = ref<{ validate: () => Promise<boolean> | undefined }>()
const categoryFormRef = ref<{ validate: () => Promise<boolean> | undefined }>()
const bankTreeRef = ref()
const bankKeyword = ref('')

const query = reactive({ page: 1, size: 20, keyword: '' })
const categoryForm = reactive<QuestionCategoryPayload>({ name: '', description: '', sortOrder: 0 })
const bankForm = reactive<QuestionBankPayload>({ categoryId: 1, name: '', description: '', status: 'ACTIVE' })
const questionForm = reactive<QuestionPayload>({
  bankId: 1,
  type: 'SINGLE_CHOICE',
  stem: '',
  analysis: '',
  difficulty: 'EASY',
  status: 'ACTIVE',
  options: [
    { label: 'A', content: '', correct: true },
    { label: 'B', content: '', correct: false },
  ],
  attachments: [],
})

const bankRules: FormRules<QuestionBankPayload> = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  name: [{ required: true, message: '请输入题库名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const questionRules: FormRules<QuestionPayload> = {
  bankId: [{ required: true, message: '请选择题库', trigger: 'change' }],
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  stem: [{ required: true, message: '请输入题干', trigger: 'blur' }],
}

const categoryRules: FormRules<QuestionCategoryPayload> = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

const selectedBank = computed(() => banks.value.find((bank) => bank.id === selectedBankId.value) || null)
const selectedCategory = computed(() => categories.value.find((category) => category.id === selectedCategoryId.value) || null)
const bankQuestionTotal = computed(() => banks.value.reduce((sum, bank) => sum + bank.questionCount, 0))
const bankTree = computed<BankTreeNode[]>(() => buildBankTree(categories.value, banks.value))

onMounted(async () => {
  await Promise.all([loadCategories(), loadBanks()])
  await loadQuestions()
})

async function loadCategories() {
  categories.value = await fetchQuestionCategories()
  if (selectedCategoryId.value && !categories.value.some((category) => category.id === selectedCategoryId.value)) {
    selectedCategoryId.value = null
  }
}

async function loadBanks(preferredBankId = selectedBankId.value) {
  bankLoading.value = true
  try {
    const result = await fetchQuestionBanks({ page: 1, size: 500, keyword: bankKeyword.value || undefined })
    banks.value = result.records
    if (preferredBankId && banks.value.some((bank) => bank.id === preferredBankId)) {
      selectedBankId.value = preferredBankId
      selectedCategoryId.value = banks.value.find((bank) => bank.id === preferredBankId)?.categoryId || selectedCategoryId.value
    } else if (selectedBankId.value && !banks.value.some((bank) => bank.id === selectedBankId.value)) {
      selectedBankId.value = null
    }
    await nextTick()
    if (selectedBankId.value) {
      bankTreeRef.value?.setCurrentKey(`bank-${selectedBankId.value}`)
    } else if (selectedCategoryId.value) {
      bankTreeRef.value?.setCurrentKey(`category-${selectedCategoryId.value}`)
    }
  } finally {
    bankLoading.value = false
  }
}

async function loadQuestions() {
  loading.value = true
  try {
    const result = await fetchQuestions({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      bankId: selectedBankId.value || undefined,
    })
    questions.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function selectBankNode(node: BankTreeNode) {
  if (node.type === 'category' && node.categoryId) {
    selectedCategoryId.value = node.categoryId
    selectedBankId.value = null
    query.page = 1
    await loadQuestions()
    return
  }
  if (node.type !== 'bank' || !node.bankId) {
    return
  }
  selectedBankId.value = node.bankId
  selectedCategoryId.value = node.categoryId || banks.value.find((bank) => bank.id === node.bankId)?.categoryId || null
  query.page = 1
  await loadQuestions()
}

async function clearSelectedBank() {
  selectedBankId.value = null
  selectedCategoryId.value = null
  bankTreeRef.value?.setCurrentKey(null)
  query.page = 1
  await loadQuestions()
}

function openCreateCategoryDialog() {
  editingCategory.value = null
  categoryForm.name = ''
  categoryForm.description = ''
  categoryForm.sortOrder = nextCategorySortOrder()
  categoryDialogVisible.value = true
}

function openEditCategoryDialog(categoryId: number) {
  const category = categories.value.find((item) => item.id === categoryId)
  if (!category) {
    return
  }
  editingCategory.value = category
  categoryForm.name = category.name
  categoryForm.description = category.description || ''
  categoryForm.sortOrder = category.sortOrder
  categoryDialogVisible.value = true
}

async function submitCategory() {
  await categoryFormRef.value?.validate()
  categorySaving.value = true
  try {
    const saved = editingCategory.value
      ? await updateQuestionCategory(editingCategory.value.id, categoryForm)
      : await createQuestionCategory(categoryForm)
    ElMessage.success(editingCategory.value ? '分类已更新' : '分类已创建')
    categoryDialogVisible.value = false
    selectedCategoryId.value = saved.id
    selectedBankId.value = null
    await loadCategories()
    await loadBanks(null)
    await loadQuestions()
  } finally {
    categorySaving.value = false
  }
}

async function deleteCategory(categoryId: number) {
  const category = categories.value.find((item) => item.id === categoryId)
  if (!category) {
    return
  }
  await ElMessageBox.confirm(`确定删除分类“${category.name}”？只有空分类可以删除。`, '删除分类', {
    type: 'warning',
    confirmButtonText: '删除分类',
    cancelButtonText: '取消',
  })
  try {
    await deleteQuestionCategory(categoryId)
  } catch {
    return
  }
  ElMessage.success('分类已删除')
  if (selectedCategoryId.value === categoryId) {
    selectedCategoryId.value = null
    selectedBankId.value = null
  }
  await loadCategories()
  await loadBanks(null)
  await loadQuestions()
}

function openCreateBankDialog(categoryId?: number) {
  editingBank.value = null
  bankForm.categoryId = categoryId || selectedBank.value?.categoryId || selectedCategoryId.value || categories.value[0]?.id || 1
  bankForm.name = ''
  bankForm.description = ''
  bankForm.status = 'ACTIVE'
  bankDialogVisible.value = true
}

function openEditBankDialog(bank: QuestionBank) {
  editingBank.value = bank
  bankForm.categoryId = bank.categoryId
  bankForm.name = bank.name
  bankForm.description = bank.description || ''
  bankForm.status = bank.status
  bankDialogVisible.value = true
}

async function submitBank() {
  await bankFormRef.value?.validate()
  bankSaving.value = true
  try {
    const saved = editingBank.value ? await updateQuestionBank(editingBank.value.id, bankForm) : await createQuestionBank(bankForm)
    ElMessage.success(editingBank.value ? '题库已更新' : '题库已创建')
    bankDialogVisible.value = false
    selectedCategoryId.value = saved.categoryId
    selectedBankId.value = saved.id
    bankKeyword.value = ''
    await loadBanks(saved.id)
    await loadQuestions()
  } finally {
    bankSaving.value = false
  }
}

function openCreateQuestionDialog() {
  editingQuestion.value = null
  Object.assign(questionForm, createQuestionPayload(selectedBankId.value || banks.value.find((bank) => bank.categoryId === selectedCategoryId.value)?.id || banks.value[0]?.id || 1))
  attachmentUrl.value = ''
  attachmentMediaType.value = 'FILE'
  questionDialogVisible.value = true
}

function openEditQuestionDialog(question: Question) {
  editingQuestion.value = question
  Object.assign(questionForm, questionToPayload(question))
  attachmentUrl.value = ''
  attachmentMediaType.value = 'FILE'
  questionDialogVisible.value = true
}

function addOption() {
  const label = nextOptionLabel(questionForm.options.length)
  questionForm.options.push({ label, content: '', correct: false })
}

function removeOption(index: number) {
  questionForm.options.splice(index, 1)
}

function normalizeQuestionOptions() {
  normalizeOptionsForType(questionForm)
}

async function submitQuestion() {
  await questionFormRef.value?.validate()
  normalizeOptionsForType(questionForm)
  const optionError = questionOptionError(questionForm)
  if (optionError) {
    ElMessage.error(optionError)
    return
  }
  saving.value = true
  try {
    const payload = { ...questionForm, attachments: [...questionForm.attachments] }
    if (editingQuestion.value) {
      await updateQuestion(editingQuestion.value.id, payload)
      ElMessage.success('试题已更新')
    } else {
      await createQuestion(payload)
      ElMessage.success('试题已创建')
    }
    questionDialogVisible.value = false
    selectedBankId.value = questionForm.bankId
    await loadBanks(questionForm.bankId)
    await loadQuestions()
  } finally {
    saving.value = false
  }
}

async function downloadTemplate() {
  const blob = await downloadQuestionImportTemplate()
  downloadBlob(blob, '试题导入模板.xlsx')
}

async function handleImport(file: UploadRawFile) {
  importResult.value = await importQuestions(file)
  await loadBanks()
  await loadQuestions()
  return false
}

async function handleAttachmentUpload(file: UploadRawFile) {
  uploadingAttachment.value = true
  try {
    questionForm.attachments.push(await uploadFile(file))
    ElMessage.success('附件已上传')
  } finally {
    uploadingAttachment.value = false
  }
  return false
}

function addUrlAttachment() {
  if (!attachmentUrl.value) {
    ElMessage.error('请输入附件 URL')
    return
  }
  questionForm.attachments.push({
    fileName: attachmentUrl.value.split('/').pop() || 'attachment',
    fileUrl: attachmentUrl.value,
    mediaType: inferMediaType(attachmentUrl.value, attachmentMediaType.value),
  })
  attachmentUrl.value = ''
  attachmentMediaType.value = 'FILE'
}

function removeAttachment(index: number) {
  questionForm.attachments.splice(index, 1)
}

function moveAttachment(index: number, offset: number) {
  const target = index + offset
  if (target < 0 || target >= questionForm.attachments.length) {
    return
  }
  const [current] = questionForm.attachments.splice(index, 1)
  questionForm.attachments.splice(target, 0, current)
}

function nextCategorySortOrder() {
  return categories.value.length ? Math.max(...categories.value.map((category) => category.sortOrder)) + 10 : 10
}
</script>

<style scoped>
.question-workbench {
  display: grid;
  grid-template-columns: minmax(300px, 380px) minmax(0, 1fr);
  gap: 16px;
  min-width: 0;
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
