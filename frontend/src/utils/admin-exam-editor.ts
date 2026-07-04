import type { Exam, ExamPaperQuestion, ExamPaperQuestionPayload, ExamRulePayload, Question } from '@/api/exam-business'
import { questionTypeMeta } from '@/utils/question-types'

export interface ExamRuleForm {
  rowId: number
  bankId: number | null
  singleCount: number
  singleScore: number
  multipleCount: number
  multipleScore: number
  writingCount: number
  writingScore: number
}

export interface ExamPaperQuestionForm {
  questionId: number
  bankId: number
  bankName: string
  type: Question['type']
  stem: string
  score: number
  sortOrder: number
}

export function createDefaultRule(rowId: number): ExamRuleForm {
  return {
    rowId,
    bankId: null,
    singleCount: 0,
    singleScore: 5,
    multipleCount: 0,
    multipleScore: 5,
    writingCount: 0,
    writingScore: 15,
  }
}

export function examRulesToForms(exam: Exam): ExamRuleForm[] {
  return exam.rules.map((rule) => ({
    rowId: Date.now() + rule.id,
    bankId: rule.bankId,
    singleCount: rule.singleCount,
    singleScore: rule.singleScore,
    multipleCount: rule.multipleCount,
    multipleScore: rule.multipleScore,
    writingCount: rule.writingCount,
    writingScore: rule.writingScore,
  }))
}

export function bankStats(questions: Question[]) {
  return {
    single: questions.filter((question) => question.type === 'SINGLE_CHOICE').length,
    multiple: questions.filter((question) => question.type === 'MULTIPLE_CHOICE').length,
    writing: questions.filter((question) => question.type === 'WRITING').length,
  }
}

export function ruleScore(rule: ExamRuleForm) {
  return rule.singleCount * rule.singleScore + rule.multipleCount * rule.multipleScore + rule.writingCount * rule.writingScore
}

export function calculateTotalScore(rules: ExamRuleForm[], paperQuestions: ExamPaperQuestionForm[]) {
  if (paperQuestions.length > 0) {
    return paperQuestions.reduce((sum, question) => sum + question.score, 0)
  }
  return rules.reduce((sum, rule) => sum + ruleScore(rule), 0)
}

export function calculateTotalQuestionCount(rules: ExamRuleForm[], paperQuestions: ExamPaperQuestionForm[]) {
  if (paperQuestions.length > 0) {
    return paperQuestions.length
  }
  return rules.reduce((sum, rule) => sum + rule.singleCount + rule.multipleCount + rule.writingCount, 0)
}

export function generatePaperQuestionsFromRules(rules: ExamRuleForm[], bankQuestions: Record<number, Question[]>) {
  const generated: ExamPaperQuestionForm[] = []
  let sortOrder = 10
  for (const rule of rules) {
    const questions = rule.bankId ? bankQuestions[rule.bankId] || [] : []
    const singleQuestions = questionsByType(questions, 'SINGLE_CHOICE', rule.singleCount)
    const multipleQuestions = questionsByType(questions, 'MULTIPLE_CHOICE', rule.multipleCount)
    const writingQuestions = questionsByType(questions, 'WRITING', rule.writingCount)
    for (const question of singleQuestions) {
      generated.push(toGeneratedPaperQuestion(question, rule.singleScore, sortOrder))
      sortOrder += 10
    }
    for (const question of multipleQuestions) {
      generated.push(toGeneratedPaperQuestion(question, rule.multipleScore, sortOrder))
      sortOrder += 10
    }
    for (const question of writingQuestions) {
      generated.push(toGeneratedPaperQuestion(question, rule.writingScore, sortOrder))
      sortOrder += 10
    }
  }
  return generated
}

export function defaultManualScore(type: Question['type'], rules: ExamRuleForm[], pickerBankId: number | null) {
  const matchedRule = rules.find((rule) => {
    if (!rule.bankId || rule.bankId !== pickerBankId) {
      return false
    }
    if (type === 'SINGLE_CHOICE') {
      return rule.singleScore > 0
    }
    if (type === 'MULTIPLE_CHOICE') {
      return rule.multipleScore > 0
    }
    return rule.writingScore > 0
  })
  if (!matchedRule) {
    return questionTypeMeta(type).manualReview ? 15 : 5
  }
  if (type === 'SINGLE_CHOICE') {
    return matchedRule.singleScore
  }
  if (type === 'MULTIPLE_CHOICE') {
    return matchedRule.multipleScore
  }
  return matchedRule.writingScore
}

export function toGeneratedPaperQuestion(question: Question, score: number, sortOrder: number): ExamPaperQuestionForm {
  return {
    questionId: question.id,
    bankId: question.bankId,
    bankName: question.bankName,
    type: question.type,
    stem: question.stem,
    score,
    sortOrder,
  }
}

export function toPaperQuestionForm(question: ExamPaperQuestion): ExamPaperQuestionForm {
  return {
    questionId: question.questionId,
    bankId: question.bankId,
    bankName: question.bankName,
    type: question.type,
    stem: question.stem,
    score: question.score,
    sortOrder: question.sortOrder,
  }
}

export function normalizePaperSort(rows: ExamPaperQuestionForm[]) {
  return rows.map((row, index) => ({
    ...row,
    sortOrder: (index + 1) * 10,
  }))
}

export function toExamRulePayloads(rules: ExamRuleForm[]): ExamRulePayload[] {
  return rules
    .filter((rule) => rule.bankId)
    .map((rule) => ({
      bankId: Number(rule.bankId),
      singleCount: rule.singleCount,
      singleScore: rule.singleCount === 0 ? 0 : rule.singleScore,
      multipleCount: rule.multipleCount,
      multipleScore: rule.multipleCount === 0 ? 0 : rule.multipleScore,
      writingCount: rule.writingCount,
      writingScore: rule.writingCount === 0 ? 0 : rule.writingScore,
    }))
}

export function toExamPaperQuestionPayloads(paperQuestions: ExamPaperQuestionForm[]): ExamPaperQuestionPayload[] {
  return paperQuestions.map((question) => ({
    questionId: question.questionId,
    score: question.score,
    sortOrder: question.sortOrder,
  }))
}

function questionsByType(questions: Question[], type: Question['type'], count: number) {
  return questions
    .filter((question) => question.type === type)
    .sort((left, right) => left.id - right.id)
    .slice(0, count)
}
