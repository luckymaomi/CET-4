import { reactive, ref, type Ref } from 'vue'
import { ElMessage, type FormRules, type UploadRawFile } from 'element-plus'

import {
  createQuestion,
  downloadQuestionImportTemplate,
  fetchQuestions,
  importQuestions,
  updateQuestion,
  uploadFile,
  type Question,
  type QuestionAttachmentPayload,
  type QuestionPayload,
} from '@/api/exam-business'
import type { ExcelImportResult } from '@/api/admin'
import { downloadBlob } from '@/utils/download'
import {
  createQuestionPayload,
  inferMediaType,
  nextOptionLabel,
  normalizeOptionsForType,
  questionOptionError,
  questionToPayload,
} from '@/utils/question-bank-editor'

export function useQuestionEditor(
  selectedBankId: Ref<number | null>,
  selectedCategoryId: Ref<number | null>,
  firstBankId: () => number,
  bankIdByCategory: (categoryId: number | null) => number | undefined,
  afterQuestionChanged: (bankId?: number) => Promise<void>,
) {
  const questions = ref<Question[]>([])
  const total = ref(0)
  const loading = ref(false)
  const saving = ref(false)
  const uploadingAttachment = ref(false)
  const questionDialogVisible = ref(false)
  const editingQuestion = ref<Question | null>(null)
  const importResult = ref<ExcelImportResult | null>(null)
  const attachmentUrl = ref('')
  const attachmentMediaType = ref<QuestionAttachmentPayload['mediaType']>('FILE')
  const query = reactive({ page: 1, size: 20, keyword: '' })

  const questionForm = reactive<QuestionPayload>(createQuestionPayload(1))
  const questionRules: FormRules<QuestionPayload> = {
    bankId: [{ required: true, message: '请选择题库', trigger: 'change' }],
    type: [{ required: true, message: '请选择题型', trigger: 'change' }],
    stem: [{ required: true, message: '请输入题干', trigger: 'blur' }],
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

  async function loadFirstPage() {
    query.page = 1
    await loadQuestions()
  }

  function openCreateQuestionDialog() {
    editingQuestion.value = null
    const bankId = selectedBankId.value || bankIdByCategory(selectedCategoryId.value) || firstBankId()
    Object.assign(questionForm, createQuestionPayload(bankId))
    resetAttachmentDraft()
    questionDialogVisible.value = true
  }

  function openEditQuestionDialog(question: Question) {
    editingQuestion.value = question
    Object.assign(questionForm, questionToPayload(question))
    resetAttachmentDraft()
    questionDialogVisible.value = true
  }

  function addOption() {
    questionForm.options.push({ label: nextOptionLabel(questionForm.options.length), content: '', correct: false })
  }

  function removeOption(index: number) {
    questionForm.options.splice(index, 1)
  }

  function normalizeQuestionOptions() {
    normalizeOptionsForType(questionForm)
  }

  async function submitQuestion(validate: () => Promise<boolean> | undefined) {
    await validate()
    normalizeOptionsForType(questionForm)
    const optionError = questionOptionError(questionForm)
    if (optionError) {
      ElMessage.error(optionError)
      return
    }
    saving.value = true
    try {
      const payload = {
        ...questionForm,
        correctLabels: questionForm.options.filter((option) => option.correct).map((option) => option.label),
        attachments: [...questionForm.attachments],
      }
      if (editingQuestion.value) {
        await updateQuestion(editingQuestion.value.id, payload)
        ElMessage.success('试题已更新')
      } else {
        await createQuestion(payload)
        ElMessage.success('试题已创建')
      }
      questionDialogVisible.value = false
      selectedBankId.value = questionForm.bankId
      await afterQuestionChanged(questionForm.bankId)
    } finally {
      saving.value = false
    }
  }

  async function downloadTemplate() {
    downloadBlob(await downloadQuestionImportTemplate(), '试题导入模板.xlsx')
  }

  async function handleImport(file: UploadRawFile) {
    importResult.value = await importQuestions(file)
    await afterQuestionChanged()
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
    resetAttachmentDraft()
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

  function resetAttachmentDraft() {
    attachmentUrl.value = ''
    attachmentMediaType.value = 'FILE'
  }

  return {
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
    loadFirstPage,
    openCreateQuestionDialog,
    openEditQuestionDialog,
    addOption,
    removeOption,
    normalizeQuestionOptions,
    submitQuestion,
    downloadTemplate,
    handleImport,
    handleAttachmentUpload,
    addUrlAttachment,
    removeAttachment,
    moveAttachment,
  }
}
