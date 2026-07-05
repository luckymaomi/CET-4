import type {
  NamedCategory,
  Question,
  QuestionAttachmentPayload,
  QuestionBank,
  QuestionPayload,
} from '@/api/exam-business'
import { questionTypeMeta } from '@/utils/question-types'

export interface BankTreeNode {
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
  writingCount: number
}

const imageUrlPattern = /\.(png|jpe?g|gif|webp|bmp|svg)(\?.*)?$/i

export function buildBankTree(categories: NamedCategory[], banks: QuestionBank[]): BankTreeNode[] {
  return categories.map((category) => ({
    key: `category-${category.id}`,
    type: 'category',
    label: category.name,
    categoryId: category.id,
    questionCount: 0,
    singleChoiceCount: 0,
    multipleChoiceCount: 0,
    writingCount: 0,
    children: banks
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
        writingCount: bank.writingCount,
        children: [],
      })),
  }))
}

export function createQuestionPayload(bankId: number): QuestionPayload {
  return {
    bankId,
    type: 'SINGLE_CHOICE',
    stem: '',
    analysis: '',
    difficulty: 'EASY',
    status: 'ACTIVE',
    options: defaultOptions(),
    correctLabels: ['A'],
    attachments: [],
  }
}

export function questionToPayload(question: Question): QuestionPayload {
  return {
    bankId: question.bankId,
    type: question.type,
    stem: question.stem,
    analysis: question.analysis || '',
    difficulty: question.difficulty,
    status: question.status,
    options: question.options.map((option) => ({ label: option.label, content: option.content, correct: option.correct })),
    correctLabels: question.options.filter((option) => option.correct).map((option) => option.label),
    attachments: question.attachments.map((attachment) => ({
      fileName: attachment.fileName,
      fileUrl: attachment.fileUrl,
      mediaType: attachment.mediaType,
    })),
  }
}

export function normalizeOptionsForType(form: QuestionPayload) {
  if (!questionTypeMeta(form.type).optionBased) {
    form.options = []
    return
  }
  if (form.options.length === 0) {
    form.options = defaultOptions()
  }
}

export function questionOptionError(form: QuestionPayload) {
  if (!questionTypeMeta(form.type).optionBased) {
    return null
  }
  const correctCount = form.options.filter((option) => option.correct).length
  const meta = questionTypeMeta(form.type)
  if (meta.optionBased && !meta.multiple && correctCount !== 1) {
    return `${meta.label}必须且只能有一个正确答案`
  }
  if (meta.multiple && correctCount < 2) {
    return '多选题至少需要两个正确答案'
  }
  return null
}

export function mediaTypeText(type: QuestionAttachmentPayload['mediaType']) {
  const names: Record<QuestionAttachmentPayload['mediaType'], string> = {
    IMAGE: '图片',
    AUDIO: '音频',
    VIDEO: '视频',
    FILE: '文件',
  }
  return names[type]
}

export function isImageAttachment(attachment: QuestionAttachmentPayload) {
  return attachment.mediaType === 'IMAGE' || imageUrlPattern.test(attachment.fileUrl)
}

export function inferMediaType(url: string, fallback: QuestionAttachmentPayload['mediaType']) {
  if (imageUrlPattern.test(url)) {
    return 'IMAGE'
  }
  return fallback
}

export function nextOptionLabel(optionCount: number) {
  return String.fromCharCode(65 + optionCount)
}

function defaultOptions() {
  return [
    { label: 'A', content: '', correct: true },
    { label: 'B', content: '', correct: false },
  ]
}
