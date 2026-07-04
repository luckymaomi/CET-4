import { describe, expect, it } from 'vitest'

import {
  calculateTotalQuestionCount,
  calculateTotalScore,
  defaultManualScore,
  generatePaperQuestionsFromRules,
  normalizePaperSort,
  toExamRulePayloads,
  type ExamPaperQuestionForm,
  type ExamRuleForm,
} from './admin-exam-editor'
import type { Question } from '@/api/exam-business'

describe('admin exam editor rules', () => {
  it('calculates totals from explicit paper questions before rules', () => {
    const rules = [rule({ singleCount: 2, singleScore: 5 })]
    const paperQuestions: ExamPaperQuestionForm[] = [
      paperQuestion({ questionId: 1, score: 8 }),
      paperQuestion({ questionId: 2, score: 12 }),
    ]

    expect(calculateTotalScore(rules, paperQuestions)).toBe(20)
    expect(calculateTotalQuestionCount(rules, paperQuestions)).toBe(2)
  })

  it('generates paper questions by type order and normalizes payload scores for zero counts', () => {
    const rules = [
      rule({ bankId: 1, singleCount: 1, singleScore: 4, multipleCount: 1, multipleScore: 6, writingCount: 1, writingScore: 20 }),
      rule({ bankId: 2, singleCount: 0, singleScore: 5, writingCount: 1, writingScore: 15 }),
    ]
    const generated = generatePaperQuestionsFromRules(rules, {
      1: [
        question({ id: 11, type: 'WRITING' }),
        question({ id: 12, type: 'SINGLE_CHOICE' }),
        question({ id: 13, type: 'MULTIPLE_CHOICE' }),
      ],
      2: [question({ id: 21, bankId: 2, type: 'WRITING' })],
    })

    expect(generated.map((item) => [item.questionId, item.score, item.sortOrder])).toEqual([
      [12, 4, 10],
      [13, 6, 20],
      [11, 20, 30],
      [21, 15, 40],
    ])
    expect(toExamRulePayloads(rules)[1].singleScore).toBe(0)
  })

  it('uses rule score for manual questions and keeps sort order contiguous', () => {
    const rules = [rule({ bankId: 3, writingScore: 18 })]

    expect(defaultManualScore('WRITING', rules, 3)).toBe(18)
    expect(defaultManualScore('MULTIPLE_CHOICE', [], null)).toBe(5)
    expect(normalizePaperSort([paperQuestion({ sortOrder: 90 }), paperQuestion({ questionId: 2, sortOrder: 10 })]).map((item) => item.sortOrder)).toEqual([10, 20])
  })
})

function rule(overrides: Partial<ExamRuleForm> = {}): ExamRuleForm {
  return {
    rowId: 1,
    bankId: 1,
    singleCount: 0,
    singleScore: 5,
    multipleCount: 0,
    multipleScore: 5,
    writingCount: 0,
    writingScore: 15,
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
    options: [],
    attachments: [],
    ...overrides,
  }
}

function paperQuestion(overrides: Partial<ExamPaperQuestionForm> = {}): ExamPaperQuestionForm {
  return {
    questionId: 1,
    bankId: 1,
    bankName: '题库',
    type: 'SINGLE_CHOICE',
    stem: '题干',
    score: 5,
    sortOrder: 10,
    ...overrides,
  }
}
