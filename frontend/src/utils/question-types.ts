export type QuestionTypeCode = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'WRITING'

export interface QuestionTypeMeta {
  code: QuestionTypeCode
  label: string
  shortLabel: string
  optionBased: boolean
  multiple: boolean
  manualReview: boolean
}

export const questionTypes: QuestionTypeMeta[] = [
  { code: 'SINGLE_CHOICE', label: '单选题', shortLabel: '单选', optionBased: true, multiple: false, manualReview: false },
  { code: 'MULTIPLE_CHOICE', label: '多选题', shortLabel: '多选', optionBased: true, multiple: true, manualReview: false },
  { code: 'WRITING', label: '写作题', shortLabel: '写作', optionBased: false, multiple: false, manualReview: true },
]

export function questionTypeMeta(type: QuestionTypeCode) {
  return questionTypes.find((item) => item.code === type) || questionTypes[0]
}

export function questionTypeText(type: QuestionTypeCode) {
  return questionTypeMeta(type).shortLabel
}
