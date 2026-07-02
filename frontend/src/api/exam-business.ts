import { apiClient } from './client'
import type { ApiResponse } from './types'
import type { ExcelImportResult, PageResult } from './admin'

export interface NamedCategory {
  id: number
  name: string
  description: string | null
  sortOrder: number
}

export interface QuestionBank {
  id: number
  categoryId: number
  categoryName: string
  name: string
  description: string | null
  status: 'ACTIVE' | 'DISABLED'
}

export interface QuestionBankPayload {
  categoryId: number
  name: string
  description: string
  status: QuestionBank['status']
}

export interface QuestionOptionPayload {
  label: string
  content: string
  correct: boolean
}

export interface QuestionOption {
  id: number
  label: string
  content: string
  correct: boolean
  sortOrder: number
}

export interface QuestionAttachmentPayload {
  fileName: string
  fileUrl: string
  mediaType: 'IMAGE' | 'AUDIO' | 'VIDEO' | 'FILE'
}

export interface QuestionAttachment extends QuestionAttachmentPayload {
  id: number
  sortOrder: number
}

export interface QuestionPayload {
  bankId: number
  type: 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE'
  stem: string
  analysis: string
  difficulty: 'EASY' | 'HARD'
  status: 'ACTIVE' | 'DISABLED'
  options: QuestionOptionPayload[]
  attachments: QuestionAttachmentPayload[]
}

export interface Question {
  id: number
  bankId: number
  bankName: string
  type: QuestionPayload['type']
  stem: string
  analysis: string | null
  difficulty: QuestionPayload['difficulty']
  status: QuestionPayload['status']
  options: QuestionOption[]
  attachments: QuestionAttachment[]
}

export interface ExamPayload {
  title: string
  description: string
  qualifyScore: number
  startTime: string
  endTime: string
  durationMinutes: number
  timeLimit: boolean
  attemptLimit: number | null
  displayMode: 'PAGED' | 'ALL'
  questionOrderMode: 'FIXED' | 'RANDOM'
  openType: 'PUBLIC' | 'DEPARTMENT'
  departmentIds: number[]
  rules: ExamRulePayload[]
}

export interface ExamRulePayload {
  bankId: number
  singleCount: number
  singleScore: number
  multipleCount: number
  multipleScore: number
}

export interface ExamRule extends ExamRulePayload {
  id: number
  bankName: string
  sortOrder: number
}

export interface Exam {
  id: number
  totalScore: number
  questionCount: number
  title: string
  description: string | null
  qualifyScore: number
  startTime: string
  endTime: string
  durationMinutes: number
  timeLimit: boolean
  attemptLimit: number | null
  displayMode: ExamPayload['displayMode']
  questionOrderMode: ExamPayload['questionOrderMode']
  openType: ExamPayload['openType']
  departmentIds: number[]
  rules: ExamRule[]
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED'
}

export interface ExamQuestionOption {
  id: number
  label: string
  content: string
  sortOrder: number
}

export interface ExamQuestion {
  questionId: number
  type: QuestionPayload['type']
  stem: string
  score: number
  sortOrder: number
  attachments: QuestionAttachment[]
  options: ExamQuestionOption[]
}

export interface ExamSession {
  examId: number
  attemptId: number
  title: string
  durationMinutes: number
  displayMode: ExamPayload['displayMode']
  startedAt: string
  attemptStatus: 'IN_PROGRESS' | 'SUBMITTED'
  questions: ExamQuestion[]
}

export interface ExamResult {
  id: number
  attemptId: number
  examId: number
  examTitle: string
  userId: number
  totalScore: number
  obtainedScore: number
  correctCount: number
  questionCount: number
  submittedAt: string
}

export interface ExamResultQuestion {
  questionId: number
  type: QuestionPayload['type']
  stem: string
  analysis: string | null
  score: number
  obtainedScore: number
  sortOrder: number
  selectedLabels: string[]
  correctLabels: string[]
  correct: boolean
  attachments: QuestionAttachment[]
  options: ExamQuestionOption[]
}

export interface ExamResultDetail extends ExamResult {
  questions: ExamResultQuestion[]
}

export async function fetchQuestionCategories(): Promise<NamedCategory[]> {
  const response = await apiClient.get<ApiResponse<NamedCategory[]>>('/api/admin/question-banks/categories')
  return response.data.data
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

export async function closeExam(id: number): Promise<Exam> {
  const response = await apiClient.post<ApiResponse<Exam>>(`/api/admin/exams/${id}/close`)
  return response.data.data
}

export async function fetchAdminResults(): Promise<ExamResult[]> {
  const response = await apiClient.get<ApiResponse<ExamResult[]>>('/api/admin/results')
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

export async function submitExam(examId: number, answers: Array<{ questionId: number; selectedLabels: string[] }>): Promise<ExamResult> {
  const response = await apiClient.post<ApiResponse<ExamResult>>(`/api/exam/${examId}/submit`, { answers })
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
