import { apiClient } from './client'
import type { ApiResponse } from './types'
import type {
  ExcelImportResult,
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
} from './exam-business-types'

export type {
  Exam,
  ExamPaperQuestion,
  ExamPaperQuestionPayload,
  ExamPayload,
  ExamQuestion,
  ExamQuestionOption,
  ExamResult,
  ExamResultDetail,
  ExamResultQuestion,
  ExamRule,
  ExamRulePayload,
  ExamSession,
  NamedCategory,
  Question,
  QuestionAttachment,
  QuestionAttachmentPayload,
  QuestionBank,
  QuestionBankPayload,
  QuestionCategoryPayload,
  QuestionOption,
  QuestionOptionPayload,
  QuestionPayload,
} from './exam-business-types'

export async function fetchQuestionCategories(): Promise<NamedCategory[]> {
  const response = await apiClient.get<ApiResponse<NamedCategory[]>>('/api/admin/question-banks/categories')
  return response.data.data
}

export async function createQuestionCategory(payload: QuestionCategoryPayload): Promise<NamedCategory> {
  const response = await apiClient.post<ApiResponse<NamedCategory>>('/api/admin/question-banks/categories', payload)
  return response.data.data
}

export async function updateQuestionCategory(id: number, payload: QuestionCategoryPayload): Promise<NamedCategory> {
  const response = await apiClient.put<ApiResponse<NamedCategory>>(`/api/admin/question-banks/categories/${id}`, payload)
  return response.data.data
}

export async function deleteQuestionCategory(id: number): Promise<void> {
  await apiClient.delete<ApiResponse<void>>(`/api/admin/question-banks/categories/${id}`)
}

export async function fetchQuestionBanks(params: { page: number; size: number; keyword?: string }): Promise<PageResult<QuestionBank>> {
  const response = await apiClient.get<ApiResponse<PageResult<QuestionBank>>>('/api/admin/question-banks', { params })
  return response.data.data
}

export async function createQuestionBank(payload: QuestionBankPayload): Promise<QuestionBank> {
  const response = await apiClient.post<ApiResponse<QuestionBank>>('/api/admin/question-banks', payload)
  return response.data.data
}

export async function updateQuestionBank(id: number, payload: QuestionBankPayload): Promise<QuestionBank> {
  const response = await apiClient.put<ApiResponse<QuestionBank>>(`/api/admin/question-banks/${id}`, payload)
  return response.data.data
}

export async function fetchQuestions(params: { page: number; size: number; keyword?: string; bankId?: number }): Promise<PageResult<Question>> {
  const response = await apiClient.get<ApiResponse<PageResult<Question>>>('/api/admin/questions', { params })
  return response.data.data
}

export async function fetchQuestionDetail(id: number): Promise<Question> {
  const response = await apiClient.get<ApiResponse<Question>>(`/api/admin/questions/${id}`)
  return response.data.data
}

export async function createQuestion(payload: QuestionPayload): Promise<Question> {
  const response = await apiClient.post<ApiResponse<Question>>('/api/admin/questions', payload)
  return response.data.data
}

export async function updateQuestion(id: number, payload: QuestionPayload): Promise<Question> {
  const response = await apiClient.put<ApiResponse<Question>>(`/api/admin/questions/${id}`, payload)
  return response.data.data
}

export async function downloadQuestionImportTemplate(): Promise<Blob> {
  const response = await apiClient.get('/api/admin/questions/import-template', { responseType: 'blob' })
  return response.data
}

export async function importQuestions(file: File): Promise<ExcelImportResult> {
  const form = new FormData()
  form.append('file', file)
  const response = await apiClient.post<ApiResponse<ExcelImportResult>>('/api/admin/questions/import', form)
  return response.data.data
}

export async function downloadQuestionExport(): Promise<Blob> {
  const response = await apiClient.get('/api/admin/questions/export', { responseType: 'blob' })
  return response.data
}

export async function uploadFile(file: File): Promise<QuestionAttachmentPayload> {
  const form = new FormData()
  form.append('file', file)
  const response = await apiClient.post<ApiResponse<QuestionAttachmentPayload>>('/api/admin/files', form)
  return response.data.data
}

export async function fetchAdminExams(params: { page: number; size: number; keyword?: string }): Promise<PageResult<Exam>> {
  const response = await apiClient.get<ApiResponse<PageResult<Exam>>>('/api/admin/exams', { params })
  return response.data.data
}

export async function fetchAdminExamDetail(id: number): Promise<Exam> {
  const response = await apiClient.get<ApiResponse<Exam>>(`/api/admin/exams/${id}`)
  return response.data.data
}

export async function createExam(payload: ExamPayload): Promise<Exam> {
  const response = await apiClient.post<ApiResponse<Exam>>('/api/admin/exams', payload)
  return response.data.data
}

export async function updateExam(id: number, payload: ExamPayload): Promise<Exam> {
  const response = await apiClient.put<ApiResponse<Exam>>(`/api/admin/exams/${id}`, payload)
  return response.data.data
}

export async function publishExam(id: number): Promise<Exam> {
  const response = await apiClient.post<ApiResponse<Exam>>(`/api/admin/exams/${id}/publish`)
  return response.data.data
}

export async function copyExam(id: number): Promise<Exam> {
  const response = await apiClient.post<ApiResponse<Exam>>(`/api/admin/exams/${id}/copy`)
  return response.data.data
}

export async function downloadExamPaper(id: number): Promise<Blob> {
  const response = await apiClient.get(`/api/admin/exams/${id}/download`, { responseType: 'blob' })
  return response.data
}

export async function revokeExam(id: number): Promise<Exam> {
  const response = await apiClient.post<ApiResponse<Exam>>(`/api/admin/exams/${id}/revoke`)
  return response.data.data
}

export async function closeExam(id: number): Promise<Exam> {
  const response = await apiClient.post<ApiResponse<Exam>>(`/api/admin/exams/${id}/close`)
  return response.data.data
}

export async function deleteExam(id: number): Promise<void> {
  await apiClient.delete<ApiResponse<void>>(`/api/admin/exams/${id}`)
}

export async function fetchAdminResults(params?: { examId?: number }): Promise<ExamResult[]> {
  const response = await apiClient.get<ApiResponse<ExamResult[]>>('/api/admin/results', { params })
  return response.data.data
}

export async function fetchAdminResultDetail(resultId: number): Promise<ExamResultDetail> {
  const response = await apiClient.get<ApiResponse<ExamResultDetail>>(`/api/admin/results/${resultId}`)
  return response.data.data
}

export async function fetchExamTasks(): Promise<Exam[]> {
  const response = await apiClient.get<ApiResponse<Exam[]>>('/api/exam/tasks')
  return response.data.data
}

export async function startExam(examId: number): Promise<ExamSession> {
  const response = await apiClient.post<ApiResponse<ExamSession>>(`/api/exam/${examId}/start`)
  return response.data.data
}

export async function saveExamAnswers(examId: number, answers: Array<{ questionId: number; selectedLabels?: string[]; answerText?: string }>): Promise<ExamSession> {
  const response = await apiClient.post<ApiResponse<ExamSession>>(`/api/exam/${examId}/answers`, { answers })
  return response.data.data
}

export async function submitExam(examId: number, answers: Array<{ questionId: number; selectedLabels?: string[]; answerText?: string }>): Promise<ExamResult> {
  const response = await apiClient.post<ApiResponse<ExamResult>>(`/api/exam/${examId}/submit`, { answers })
  return response.data.data
}

export async function reviewWritingQuestion(resultId: number, questionId: number, payload: { score: number; comment: string }): Promise<ExamResultDetail> {
  const response = await apiClient.post<ApiResponse<ExamResultDetail>>(`/api/admin/results/${resultId}/questions/${questionId}/review`, payload)
  return response.data.data
}

export async function completeResultReview(resultId: number): Promise<ExamResultDetail> {
  const response = await apiClient.post<ApiResponse<ExamResultDetail>>(`/api/admin/results/${resultId}/complete-review`)
  return response.data.data
}

export async function fetchMyExamResults(): Promise<ExamResult[]> {
  const response = await apiClient.get<ApiResponse<ExamResult[]>>('/api/exam/results')
  return response.data.data
}

export async function fetchMyExamResultDetail(resultId: number): Promise<ExamResultDetail> {
  const response = await apiClient.get<ApiResponse<ExamResultDetail>>(`/api/exam/results/${resultId}`)
  return response.data.data
}
