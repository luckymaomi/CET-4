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
      <aside class="tree-pane">
        <div class="pane-title">
          <strong>题库树</strong>
          <div class="pane-title__actions">
            <span>{{ banks.length }} 个题库 / {{ bankQuestionTotal }} 题</span>
            <el-button size="small" :icon="Plus" @click="openCreateCategoryDialog">新建分类</el-button>
          </div>
        </div>
        <div class="bank-search">
          <el-input v-model.trim="bankKeyword" clearable placeholder="搜索题库或分类" @keyup.enter="loadBanks" />
          <el-button :icon="Search" @click="loadBanks">搜索</el-button>
        </div>
        <el-tree
          ref="bankTreeRef"
          v-loading="bankLoading"
          :data="bankTree"
          node-key="key"
          default-expand-all
          highlight-current
          :expand-on-click-node="false"
          @node-click="selectBankNode"
        >
          <template #default="{ data }: { data: BankTreeNode }">
            <div class="bank-node" :class="{ 'bank-node--category': data.type === 'category' }">
              <div class="bank-node__main">
                <span class="entity-name">{{ data.label }}</span>
                <span v-if="data.type === 'bank'" class="muted-text">
                  {{ data.questionCount }} 题 · 单选 {{ data.singleChoiceCount }} · 多选 {{ data.multipleChoiceCount }}
                </span>
                <span v-else class="muted-text">{{ data.children.length }} 个题库</span>
              </div>
              <div class="bank-node__actions">
                <template v-if="data.type === 'category' && data.categoryId">
                  <el-button link type="primary" size="small" @click.stop="openCreateBankDialog(data.categoryId)">新建题库</el-button>
                  <el-button link type="primary" size="small" @click.stop="openEditCategoryDialog(data.categoryId)">编辑</el-button>
                  <el-button link type="danger" size="small" @click.stop="deleteCategory(data.categoryId)">删除</el-button>
                </template>
                <el-tag v-else size="small" effect="plain" :type="data.status === 'ACTIVE' ? 'success' : 'info'">
                  {{ data.status === 'ACTIVE' ? '启用' : '禁用' }}
                </el-tag>
              </div>
            </div>
          </template>
        </el-tree>
      </aside>

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
            <el-button v-if="selectedBank" @click="openEditBankDialog(selectedBank)">编辑题库</el-button>
            <el-button v-if="selectedCategory && !selectedBank" @click="openEditCategoryDialog(selectedCategory.id)">编辑分类</el-button>
            <el-button type="primary" :disabled="banks.length === 0" @click="openCreateQuestionDialog">新建试题</el-button>
          </div>
        </section>

        <div class="toolbar">
          <el-input v-model.trim="query.keyword" clearable placeholder="搜索题干或题库" class="toolbar__search" @keyup.enter="loadQuestions" />
          <el-button :icon="Search" @click="loadQuestions">搜索</el-button>
          <el-button v-if="selectedBankId" @click="clearSelectedBank">查看全部题库</el-button>
        </div>

        <el-table v-loading="loading" :data="questions" class="data-table" border>
          <el-table-column prop="stem" label="题干" min-width="260" show-overflow-tooltip />
          <el-table-column prop="bankName" label="题库" width="160" />
          <el-table-column label="题型" width="110">
            <template #default="{ row }: { row: Question }">{{ questionTypeText(row.type) }}</template>
          </el-table-column>
          <el-table-column label="答案" min-width="140">
            <template #default="{ row }: { row: Question }">
              <el-tag v-for="option in row.options.filter((item) => item.correct)" :key="option.id" class="answer-tag">
                {{ option.label }}
              </el-tag>
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
              <el-button link type="primary" @click="openEditQuestionDialog(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-row">
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :total="total"
            layout="total, prev, pager, next"
            @current-change="loadQuestions"
          />
        </div>
      </main>
    </div>

    <el-dialog v-model="categoryDialogVisible" :title="editingCategory ? '编辑分类' : '新建分类'" width="480px">
      <el-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules" label-width="92px">
        <el-form-item label="名称" prop="name">
          <el-input v-model.trim="categoryForm.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model.trim="categoryForm.description" type="textarea" :rows="3" maxlength="255" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="categoryForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="categorySaving" @click="submitCategory">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bankDialogVisible" :title="editingBank ? '编辑题库' : '新建题库'" width="520px">
      <el-form ref="bankFormRef" :model="bankForm" :rules="bankRules" label-width="92px">
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="bankForm.categoryId" class="form-control">
            <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model.trim="bankForm.name" maxlength="128" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model.trim="bankForm.description" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-segmented v-model="bankForm.status" :options="statusOptions" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bankDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bankSaving" @click="submitBank">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="questionDialogVisible" :title="editingQuestion ? '编辑试题' : '新建试题'" width="860px">
      <el-form ref="questionFormRef" :model="questionForm" :rules="questionRules" label-width="92px">
        <el-form-item label="题库" prop="bankId">
          <el-select v-model="questionForm.bankId" filterable class="form-control">
            <el-option v-for="bank in banks" :key="bank.id" :label="bank.name" :value="bank.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型" prop="type">
          <el-segmented v-model="questionForm.type" :options="questionTypeOptions" />
        </el-form-item>
        <el-form-item label="难度" prop="difficulty">
          <el-select v-model="questionForm.difficulty" class="form-control">
            <el-option label="简单" value="EASY" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干" prop="stem">
          <el-input v-model.trim="questionForm.stem" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="选项">
          <div class="option-editor">
            <div v-for="(option, index) in questionForm.options" :key="`${option.label}-${index}`" class="option-row">
              <el-checkbox v-model="option.correct" />
              <el-input v-model.trim="option.label" class="option-label" />
              <el-input v-model.trim="option.content" placeholder="选项内容" />
              <el-button :icon="Delete" circle :disabled="questionForm.options.length <= 2" @click="removeOption(index)" />
            </div>
            <el-button :icon="Plus" @click="addOption">增加选项</el-button>
          </div>
        </el-form-item>
        <el-form-item label="解析" prop="analysis">
          <el-input v-model.trim="questionForm.analysis" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="附件">
          <div class="attachment-editor">
            <el-upload :show-file-list="false" :before-upload="handleAttachmentUpload" accept=".jpg,.jpeg,.png,.gif,.webp,.mp3,.wav,.ogg,.mp4,.pdf">
              <el-button :icon="Upload" :loading="uploadingAttachment">上传附件</el-button>
            </el-upload>
            <div class="url-attachment">
              <el-input v-model.trim="attachmentUrl" placeholder="输入图片、音频、视频或文件 URL" />
              <el-select v-model="attachmentMediaType" class="url-attachment__type">
                <el-option label="图片" value="IMAGE" />
                <el-option label="音频" value="AUDIO" />
                <el-option label="视频" value="VIDEO" />
                <el-option label="文件" value="FILE" />
              </el-select>
              <el-button @click="addUrlAttachment">添加 URL</el-button>
            </div>
            <div v-if="questionForm.attachments.length" class="attachment-list">
              <div v-for="(attachment, index) in questionForm.attachments" :key="`${attachment.fileUrl}-${index}`" class="attachment-item">
                <div class="attachment-item__main">
                  <el-image
                    v-if="isImageAttachment(attachment)"
                    class="attachment-thumb"
                    :src="attachment.fileUrl"
                    :preview-src-list="[attachment.fileUrl]"
                    fit="cover"
                    preview-teleported
                  />
                  <el-tag v-else effect="plain">{{ mediaTypeText(attachment.mediaType) }}</el-tag>
                  <div class="attachment-meta">
                    <span>{{ attachment.fileName }}</span>
                    <a :href="attachment.fileUrl" target="_blank" rel="noreferrer">
                      {{ isImageAttachment(attachment) ? '查看原图' : '打开附件' }}
                    </a>
                  </div>
                </div>
                <div class="attachment-actions">
                  <el-button size="small" :disabled="index === 0" @click="moveAttachment(index, -1)">上移</el-button>
                  <el-button size="small" :disabled="index === questionForm.attachments.length - 1" @click="moveAttachment(index, 1)">下移</el-button>
                  <el-button :icon="Delete" circle @click="removeAttachment(index)" />
                </div>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="questionForm.status">
            <el-radio-button value="ACTIVE">启用</el-radio-button>
            <el-radio-button value="DISABLED">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="questionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitQuestion">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadRawFile } from 'element-plus'
import { Delete, Download, Plus, Search, Upload } from '@element-plus/icons-vue'

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

const pageTitle = '题库管理'

interface BankTreeNode {
  key: string
  type: 'category' | 'bank'
  label: string
  children: BankTreeNode[]
  categoryId?: number
  bankId?: number
  status?: QuestionBank['status']
  questionCount: number
  singleChoiceCount: number
  multipleChoiceCount: number
}

const statusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '禁用', value: 'DISABLED' },
]

const questionTypeOptions = [
  { label: '单选', value: 'SINGLE_CHOICE' },
  { label: '多选', value: 'MULTIPLE_CHOICE' },
]

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
const questionFormRef = ref<FormInstance>()
const bankFormRef = ref<FormInstance>()
const categoryFormRef = ref<FormInstance>()
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
const bankTree = computed<BankTreeNode[]>(() =>
  categories.value.map((category) => ({
    key: `category-${category.id}`,
    type: 'category',
    label: category.name,
    categoryId: category.id,
    questionCount: 0,
    singleChoiceCount: 0,
    multipleChoiceCount: 0,
    children: banks.value
      .filter((bank) => bank.categoryId === category.id)
      .map((bank) => ({
        key: `bank-${bank.id}`,
        type: 'bank',
        label: bank.name,
        bankId: bank.id,
        status: bank.status,
        questionCount: bank.questionCount,
        singleChoiceCount: bank.singleChoiceCount,
        multipleChoiceCount: bank.multipleChoiceCount,
        children: [],
      })),
  })),
)

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
  questionForm.bankId = selectedBankId.value || banks.value.find((bank) => bank.categoryId === selectedCategoryId.value)?.id || banks.value[0]?.id || 1
  questionForm.type = 'SINGLE_CHOICE'
  questionForm.stem = ''
  questionForm.analysis = ''
  questionForm.difficulty = 'EASY'
  questionForm.status = 'ACTIVE'
  questionForm.options = [
    { label: 'A', content: '', correct: true },
    { label: 'B', content: '', correct: false },
  ]
  questionForm.attachments = []
  attachmentUrl.value = ''
  attachmentMediaType.value = 'FILE'
  questionDialogVisible.value = true
}

function openEditQuestionDialog(question: Question) {
  editingQuestion.value = question
  questionForm.bankId = question.bankId
  questionForm.type = question.type
  questionForm.stem = question.stem
  questionForm.analysis = question.analysis || ''
  questionForm.difficulty = question.difficulty
  questionForm.status = question.status
  questionForm.options = question.options.map((option) => ({ label: option.label, content: option.content, correct: option.correct }))
  questionForm.attachments = question.attachments.map((attachment) => ({
    fileName: attachment.fileName,
    fileUrl: attachment.fileUrl,
    mediaType: attachment.mediaType,
  }))
  attachmentUrl.value = ''
  attachmentMediaType.value = 'FILE'
  questionDialogVisible.value = true
}

function addOption() {
  const label = String.fromCharCode(65 + questionForm.options.length)
  questionForm.options.push({ label, content: '', correct: false })
}

function removeOption(index: number) {
  questionForm.options.splice(index, 1)
}

async function submitQuestion() {
  await questionFormRef.value?.validate()
  const correctCount = questionForm.options.filter((option) => option.correct).length
  if (questionForm.type === 'SINGLE_CHOICE' && correctCount !== 1) {
    ElMessage.error('单选题必须且只能有一个正确答案')
    return
  }
  if (questionForm.type === 'MULTIPLE_CHOICE' && correctCount < 2) {
    ElMessage.error('多选题至少需要两个正确答案')
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

function questionTypeText(type: Question['type']) {
  return type === 'SINGLE_CHOICE' ? '单选' : '多选'
}

function mediaTypeText(type: QuestionAttachmentPayload['mediaType']) {
  const names: Record<QuestionAttachmentPayload['mediaType'], string> = {
    IMAGE: '图片',
    AUDIO: '音频',
    VIDEO: '视频',
    FILE: '文件',
  }
  return names[type]
}

function isImageAttachment(attachment: QuestionAttachmentPayload) {
  return attachment.mediaType === 'IMAGE' || imageUrlPattern.test(attachment.fileUrl)
}

const imageUrlPattern = /\.(png|jpe?g|gif|webp|bmp|svg)(\?.*)?$/i

function inferMediaType(url: string, fallback: QuestionAttachmentPayload['mediaType']) {
  if (imageUrlPattern.test(url)) {
    return 'IMAGE'
  }
  return fallback
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

.tree-pane,
.question-pane {
  min-width: 0;
  padding: 18px;
  border: 1px solid var(--ks-border);
  border-radius: var(--ks-radius);
  background: var(--ks-panel);
}

.tree-pane :deep(.el-tree-node__content) {
  height: auto;
  min-height: 54px;
  align-items: flex-start;
  padding-top: 5px;
  padding-bottom: 5px;
  border-radius: 6px;
}

.tree-pane :deep(.el-tree-node__expand-icon) {
  margin-top: 12px;
  flex: none;
}

.question-pane {
  display: grid;
  gap: 14px;
}

.pane-title,
.selected-bank,
.bank-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.pane-title {
  margin-bottom: 12px;
}

.pane-title__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.pane-title span {
  color: var(--ks-text-muted);
  font-size: 13px;
}

.bank-search {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  margin-bottom: 12px;
}

.bank-node {
  width: 100%;
  min-width: 0;
  min-height: 44px;
  padding: 2px 0;
}

.bank-node__actions {
  display: flex;
  flex: none;
  align-items: center;
  gap: 4px;
}

.bank-node__actions .el-button {
  padding: 0;
}

.bank-node__main {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.bank-node__main .entity-name,
.bank-node__main .muted-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 20px;
}

.bank-node .el-tag {
  flex: none;
}

.bank-node--category .entity-name {
  font-weight: 700;
}

.selected-bank h2 {
  margin: 0;
  font-size: 20px;
  letter-spacing: 0;
}

.import-errors {
  margin: 8px 0 0;
  padding-left: 18px;
}

.attachment-editor {
  display: grid;
  gap: 10px;
  width: 100%;
}

.url-attachment {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 120px auto;
  gap: 10px;
  align-items: center;
}

.url-attachment__type {
  width: 120px;
}

.attachment-list {
  display: grid;
  gap: 8px;
}

.attachment-item {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  border: 1px solid var(--ks-border);
  border-radius: var(--ks-radius);
}

.attachment-item__main {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.attachment-actions {
  display: flex;
  flex: none;
  align-items: center;
  gap: 6px;
}

.attachment-thumb {
  flex: none;
  width: 72px;
  height: 54px;
  overflow: hidden;
  border: 1px solid var(--ks-border);
  border-radius: 6px;
  background: var(--ks-panel-muted);
}

.attachment-meta {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.attachment-meta span,
.attachment-meta a {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-meta a {
  max-width: 320px;
  color: var(--el-color-primary);
}

@media (max-width: 900px) {
  .question-workbench,
  .url-attachment {
    grid-template-columns: 1fr;
  }

  .selected-bank {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
