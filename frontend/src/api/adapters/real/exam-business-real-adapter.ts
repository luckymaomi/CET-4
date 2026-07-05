import { apiClient } from '../../client'
import type { ExcelImportResult } from '../../admin'
import type { ExamBusinessAdapter } from '../exam-business-adapter'
import type { ApiResponse } from '../../types'
import type {
  Exam,
  ExamResult,
  ExamResultDetail,
  ExamSession,
  NamedCategory,
  PageResult,
  Question,
  QuestionAttachmentPayload,
  QuestionBank,
} from '../../exam-business-types'

export const realExamBusinessAdapter: ExamBusinessAdapter = {
  async fetchQuestionCategories() {
    const response = await apiClient.get<ApiResponse<NamedCategory[]>>('/api/admin/question-banks/categories')
    return response.data.data
  },
  async createQuestionCategory(payload) {
    const response = await apiClient.post<ApiResponse<NamedCategory>>('/api/admin/question-banks/categories', payload)
    return response.data.data
  },
  async updateQuestionCategory(id, payload) {
    const response = await apiClient.put<ApiResponse<NamedCategory>>(`/api/admin/question-banks/categories/${id}`, payload)
    return response.data.data
  },
  async deleteQuestionCategory(id) {
    await apiClient.delete<ApiResponse<void>>(`/api/admin/question-banks/categories/${id}`)
  },
  async fetchQuestionBanks(params) {
    const response = await apiClient.get<ApiResponse<PageResult<QuestionBank>>>('/api/admin/question-banks', { params })
    return response.data.data
  },
  async createQuestionBank(payload) {
    const response = await apiClient.post<ApiResponse<QuestionBank>>('/api/admin/question-banks', payload)
    return response.data.data
  },
  async updateQuestionBank(id, payload) {
    const response = await apiClient.put<ApiResponse<QuestionBank>>(`/api/admin/question-banks/${id}`, payload)
    return response.data.data
  },
  async fetchQuestions(params) {
    const response = await apiClient.get<ApiResponse<PageResult<Question>>>('/api/admin/questions', { params })
    return response.data.data
  },
  async fetchQuestionDetail(id) {
    const response = await apiClient.get<ApiResponse<Question>>(`/api/admin/questions/${id}`)
    return response.data.data
  },
  async createQuestion(payload) {
    const response = await apiClient.post<ApiResponse<Question>>('/api/admin/questions', payload)
    return response.data.data
  },
  async updateQuestion(id, payload) {
    const response = await apiClient.put<ApiResponse<Question>>(`/api/admin/questions/${id}`, payload)
    return response.data.data
  },
  async downloadQuestionImportTemplate() {
    const response = await apiClient.get('/api/admin/questions/import-template', { responseType: 'blob' })
    return response.data
  },
  async importQuestions(file) {
    const form = new FormData()
    form.append('file', file)
    const response = await apiClient.post<ApiResponse<ExcelImportResult>>('/api/admin/questions/import', form)
    return response.data.data
  },
  async downloadQuestionExport() {
    const response = await apiClient.get('/api/admin/questions/export', { responseType: 'blob' })
    return response.data
  },
  async uploadFile(file) {
    const form = new FormData()
    form.append('file', file)
    const response = await apiClient.post<ApiResponse<QuestionAttachmentPayload>>('/api/admin/files', form)
    return response.data.data
  },
  async fetchAdminExams(params) {
    const response = await apiClient.get<ApiResponse<PageResult<Exam>>>('/api/admin/exams', { params })
    return response.data.data
  },
  async fetchAdminExamDetail(id) {
    const response = await apiClient.get<ApiResponse<Exam>>(`/api/admin/exams/${id}`)
    return response.data.data
  },
  async createExam(payload) {
    const response = await apiClient.post<ApiResponse<Exam>>('/api/admin/exams', payload)
    return response.data.data
  },
  async updateExam(id, payload) {
    const response = await apiClient.put<ApiResponse<Exam>>(`/api/admin/exams/${id}`, payload)
    return response.data.data
  },
  async publishExam(id) {
    const response = await apiClient.post<ApiResponse<Exam>>(`/api/admin/exams/${id}/publish`)
    return response.data.data
  },
  async copyExam(id) {
    const response = await apiClient.post<ApiResponse<Exam>>(`/api/admin/exams/${id}/copy`)
    return response.data.data
  },
  async downloadExamPaper(id) {
    const response = await apiClient.get(`/api/admin/exams/${id}/download`, { responseType: 'blob' })
    return response.data
  },
  async revokeExam(id) {
    const response = await apiClient.post<ApiResponse<Exam>>(`/api/admin/exams/${id}/revoke`)
    return response.data.data
  },
  async closeExam(id) {
    const response = await apiClient.post<ApiResponse<Exam>>(`/api/admin/exams/${id}/close`)
    return response.data.data
  },
  async deleteExam(id) {
    await apiClient.delete<ApiResponse<void>>(`/api/admin/exams/${id}`)
  },
  async fetchAdminResults(params) {
    const response = await apiClient.get<ApiResponse<ExamResult[]>>('/api/admin/results', { params })
    return response.data.data
  },
  async fetchAdminResultDetail(resultId) {
    const response = await apiClient.get<ApiResponse<ExamResultDetail>>(`/api/admin/results/${resultId}`)
    return response.data.data
  },
  async fetchExamTasks() {
    const response = await apiClient.get<ApiResponse<Exam[]>>('/api/exam/tasks')
    return response.data.data
  },
  async startExam(examId) {
    const response = await apiClient.post<ApiResponse<ExamSession>>(`/api/exam/${examId}/start`)
    return response.data.data
  },
  async saveExamAnswers(examId, answers) {
    const response = await apiClient.post<ApiResponse<ExamSession>>(`/api/exam/${examId}/answers`, { answers })
    return response.data.data
  },
  async submitExam(examId, answers) {
    const response = await apiClient.post<ApiResponse<ExamResult>>(`/api/exam/${examId}/submit`, { answers })
    return response.data.data
  },
  async reviewWritingQuestion(resultId, questionId, payload) {
    const response = await apiClient.post<ApiResponse<ExamResultDetail>>(`/api/admin/results/${resultId}/questions/${questionId}/review`, payload)
    return response.data.data
  },
  async completeResultReview(resultId) {
    const response = await apiClient.post<ApiResponse<ExamResultDetail>>(`/api/admin/results/${resultId}/complete-review`)
    return response.data.data
  },
  async fetchMyExamResults() {
    const response = await apiClient.get<ApiResponse<ExamResult[]>>('/api/exam/results')
    return response.data.data
  },
  async fetchMyExamResultDetail(resultId) {
    const response = await apiClient.get<ApiResponse<ExamResultDetail>>(`/api/exam/results/${resultId}`)
    return response.data.data
  },
}
