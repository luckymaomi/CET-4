import type { ExcelImportResult } from '../admin'
import type {
  Exam,
  ExamPayload,
  ExamResult,
  ExamResultDetail,
  ExamSession,
  NamedCategory,
  PageResult,
  Question,
  QuestionAttachmentPayload,
  QuestionBank,
  QuestionBankPayload,
  QuestionCategoryPayload,
  QuestionPayload,
} from '../exam-business-types'

export interface ExamBusinessAdapter {
  fetchQuestionCategories(): Promise<NamedCategory[]>
  createQuestionCategory(payload: QuestionCategoryPayload): Promise<NamedCategory>
  updateQuestionCategory(id: number, payload: QuestionCategoryPayload): Promise<NamedCategory>
  deleteQuestionCategory(id: number): Promise<void>
  fetchQuestionBanks(params: { page: number; size: number; keyword?: string }): Promise<PageResult<QuestionBank>>
  createQuestionBank(payload: QuestionBankPayload): Promise<QuestionBank>
  updateQuestionBank(id: number, payload: QuestionBankPayload): Promise<QuestionBank>
  fetchQuestions(params: { page: number; size: number; keyword?: string; bankId?: number }): Promise<PageResult<Question>>
  fetchQuestionDetail(id: number): Promise<Question>
  createQuestion(payload: QuestionPayload): Promise<Question>
  updateQuestion(id: number, payload: QuestionPayload): Promise<Question>
  downloadQuestionImportTemplate(): Promise<Blob>
  importQuestions(file: File): Promise<ExcelImportResult>
  downloadQuestionExport(): Promise<Blob>
  uploadFile(file: File): Promise<QuestionAttachmentPayload>
  fetchAdminExams(params: { page: number; size: number; keyword?: string }): Promise<PageResult<Exam>>
  fetchAdminExamDetail(id: number): Promise<Exam>
  createExam(payload: ExamPayload): Promise<Exam>
  updateExam(id: number, payload: ExamPayload): Promise<Exam>
  publishExam(id: number): Promise<Exam>
  copyExam(id: number): Promise<Exam>
  downloadExamPaper(id: number): Promise<Blob>
  revokeExam(id: number): Promise<Exam>
  closeExam(id: number): Promise<Exam>
  deleteExam(id: number): Promise<void>
  fetchAdminResults(params?: { examId?: number }): Promise<ExamResult[]>
  fetchAdminResultDetail(resultId: number): Promise<ExamResultDetail>
  fetchExamTasks(): Promise<Exam[]>
  startExam(examId: number): Promise<ExamSession>
  saveExamAnswers(examId: number, answers: Array<{ questionId: number; selectedLabels?: string[]; answerText?: string }>): Promise<ExamSession>
  submitExam(examId: number, answers: Array<{ questionId: number; selectedLabels?: string[]; answerText?: string }>): Promise<ExamResult>
  reviewWritingQuestion(resultId: number, questionId: number, payload: { score: number; comment: string }): Promise<ExamResultDetail>
  completeResultReview(resultId: number): Promise<ExamResultDetail>
  fetchMyExamResults(): Promise<ExamResult[]>
  fetchMyExamResultDetail(resultId: number): Promise<ExamResultDetail>
}
