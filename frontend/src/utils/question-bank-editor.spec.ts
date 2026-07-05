import { describe, expect, it } from 'vitest'

import {
  buildBankTree,
  createQuestionPayload,
  inferMediaType,
  isImageAttachment,
  nextOptionLabel,
  normalizeOptionsForType,
  questionOptionError,
  questionToPayload,
} from './question-bank-editor'
import type { NamedCategory, Question, QuestionBank, QuestionPayload } from '@/api/exam-business'

describe('question bank editor rules', () => {
  it('builds category and bank tree nodes from flat records', () => {
    const tree = buildBankTree([category()], [bank({ id: 3, categoryId: 1, name: '英语题库' })])

    expect(tree).toHaveLength(1)
    expect(tree[0].children[0]).toMatchObject({
      key: 'bank-3',
      type: 'bank',
      label: '英语题库',
      questionCount: 4,
    })
  })

  it('normalizes options and validates answer rules by question type', () => {
    const writing = createQuestionPayload(1)
    writing.type = 'WRITING'
    normalizeOptionsForType(writing)
    expect(writing.options).toEqual([])

    const single = createQuestionPayload(1)
    single.options.forEach((option) => {
      option.correct = true
    })
    expect(questionOptionError(single)).toBe('单选题必须且只能有一个正确答案')

    const multiple: QuestionPayload = { ...single, type: 'MULTIPLE_CHOICE', options: single.options.map((option) => ({ ...option, correct: option.label === 'A' })) }
    expect(questionOptionError(multiple)).toBe('多选题至少需要两个正确答案')
  })

  it('maps edit payloads and infers attachment media type', () => {
    expect(nextOptionLabel(2)).toBe('C')
    expect(inferMediaType('/files/chart.png', 'FILE')).toBe('IMAGE')
    expect(isImageAttachment({ fileName: 'chart', fileUrl: '/files/chart.jpg', mediaType: 'FILE' })).toBe(true)
    expect(questionToPayload(question()).attachments[0]).toEqual({ fileName: 'chart.png', fileUrl: '/chart.png', mediaType: 'IMAGE' })
  })

})

function category(overrides: Partial<NamedCategory> = {}): NamedCategory {
  return {
    id: 1,
    name: '默认分类',
    description: null,
    sortOrder: 10,
    ...overrides,
  }
}

function bank(overrides: Partial<QuestionBank> = {}): QuestionBank {
  return {
    id: 1,
    categoryId: 1,
    categoryName: '默认分类',
    name: '题库',
    description: null,
    status: 'ACTIVE',
    questionCount: 4,
    singleChoiceCount: 2,
    multipleChoiceCount: 1,
    writingCount: 1,
    ...overrides,
  }
}

function question(overrides: Partial<Question> = {}): Question {
  return {
    id: 1,
    bankId: 1,
    bankName: '题库',
    type: 'SINGLE_CHOICE',
    stem: '题干',
    analysis: null,
    difficulty: 'EASY',
    status: 'ACTIVE',
    options: [{ id: 1, label: 'A', content: 'A', correct: true, sortOrder: 10 }],
    attachments: [{ id: 1, fileName: 'chart.png', fileUrl: '/chart.png', mediaType: 'IMAGE', sortOrder: 10 }],
    ...overrides,
  }
}
