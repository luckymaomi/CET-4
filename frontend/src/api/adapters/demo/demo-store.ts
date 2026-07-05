import type {
  AdminMenu,
  AdminPermission,
  AdminRole,
  AdminUser,
  Department,
} from '../../admin'
import type {
  Exam,
  ExamQuestion,
  ExamResultDetail,
  ExamResultQuestion,
  Question,
  QuestionAttachment,
  QuestionBank,
  QuestionOption,
} from '../../exam-business-types'
import type { CurrentUser } from '../../types'
import { isManualReviewType } from '@/utils/question-types'

interface DemoUser extends AdminUser {
  password: string
  roleIds: number[]
}

export interface DemoAttempt {
  id: number
  examId: number
  userId: number
  status: 'IN_PROGRESS' | 'SUBMITTED'
  startedAt: string
  questions: ExamQuestion[]
}

export interface DemoState {
  currentUserId: number | null
  departments: Department[]
  users: DemoUser[]
  roles: AdminRole[]
  permissions: AdminPermission[]
  menus: AdminMenu[]
  categories: Array<{ id: number; code: string; name: string; description: string | null; sortOrder: number }>
  banks: QuestionBank[]
  questions: Question[]
  correctLabelsByQuestionId: Record<number, string[]>
  exams: Exam[]
  attempts: DemoAttempt[]
  results: ExamResultDetail[]
  nextId: number
}

function clone<T>(value: T): T {
  return structuredClone(value)
}

function nowIso() {
  return new Date().toISOString()
}

function nextId(state: Pick<DemoState, 'nextId'>) {
  state.nextId += 1
  return state.nextId
}

function seedState(): DemoState {
  const state: DemoState = {
    currentUserId: null,
    departments: [
      { id: 1, parentId: null, name: '默认组织', code: 'ROOT', description: '演示组织根节点', status: 'ACTIVE', children: [] },
      { id: 2, parentId: 1, name: '教学部', code: 'TEACHING', description: '演示教学部门', status: 'ACTIVE', children: [] },
    ],
    users: [],
    roles: [],
    permissions: [],
    menus: [],
    categories: [{ id: 101, code: 'cet4-demo', name: '四级样例', description: '大学英语四级演示题库分类', sortOrder: 10 }],
    banks: [],
    questions: [],
    correctLabelsByQuestionId: {},
    exams: [],
    attempts: [],
    results: [],
    nextId: 1000,
  }
  state.departments[0].children = [state.departments[1]]
  state.permissions = buildPermissions()
  state.menus = buildMenus()
  state.roles = buildRoles(state.permissions, state.menus)
  state.users = buildUsers()
  buildQuestionBank(state)
  buildExam(state)
  seedSubmittedResult(state)
  return state
}

function buildPermissions(): AdminPermission[] {
  const codes = [
    ['admin:users', '用户管理'],
    ['admin:roles', '角色管理'],
    ['admin:departments', '部门管理'],
    ['exam:questions', '题库管理'],
    ['exam:manage', '考试管理'],
    ['exam:review', '成绩阅卷'],
    ['exam:take', '在线考试'],
  ]
  return codes.map(([code, name], index) => ({ id: index + 1, code, name, description: `${name}权限` }))
}

function buildMenus(): AdminMenu[] {
  return [
    { id: 1, code: 'online-exam', title: '在线考试', path: '/my/exam', parentId: null, sortOrder: 10, icon: null },
    { id: 2, code: 'exam-management', title: '考试管理', path: '/exam/manage', parentId: null, sortOrder: 20, icon: null },
    { id: 3, code: 'system-management', title: '系统管理', path: '/sys/roles', parentId: null, sortOrder: 30, icon: null },
  ]
}

function buildRoles(permissions: AdminPermission[], menus: AdminMenu[]): AdminRole[] {
  return [
    { id: 1, code: 'ADMIN', name: '系统管理员', description: '拥有演示环境全部权限', permissions, menus },
    { id: 2, code: 'STUDENT', name: '考生', description: '参加考试并查看成绩', permissions: permissions.filter((item) => item.code === 'exam:take'), menus: menus.slice(0, 1) },
  ]
}

function buildUsers(): DemoUser[] {
  const timestamp = nowIso()
  return [
    { id: 1, departmentId: 1, departmentName: '默认组织', username: 'admin', displayName: '系统管理员', status: 'ACTIVE', roles: ['ADMIN'], roleIds: [1], password: 'password', createdAt: timestamp, updatedAt: timestamp },
    { id: 2, departmentId: 2, departmentName: '教学部', username: 'zhangsan', displayName: '张三', status: 'ACTIVE', roles: ['STUDENT'], roleIds: [2], password: 'password', createdAt: timestamp, updatedAt: timestamp },
  ]
}

function buildQuestionBank(state: DemoState) {
  const bankId = nextId(state)
  state.banks.push({
    id: bankId,
    categoryId: state.categories[0].id,
    categoryName: state.categories[0].name,
    name: '四级样例题库',
    description: '用于演示单选、多选和写作题维护流程',
    status: 'ACTIVE',
    questionCount: 0,
    singleChoiceCount: 0,
    multipleChoiceCount: 0,
    writingCount: 0,
  })

  const questions: Question[] = [
    choiceQuestion(state, bankId, 'SINGLE_CHOICE', 'The lecture mainly discusses the importance of regular practice.', ['True', 'False', 'Not given', 'Unknown'], ['A'], '听力材料中的核心观点是持续练习。'),
    choiceQuestion(state, bankId, 'SINGLE_CHOICE', 'Which word is closest in meaning to "essential"?', ['necessary', 'optional', 'ordinary', 'temporary'], ['A'], 'essential 表示必要的。'),
    choiceQuestion(state, bankId, 'MULTIPLE_CHOICE', 'Which of the following are effective study habits?', ['Reviewing notes', 'Planning study time', 'Ignoring feedback', 'Practicing with past papers'], ['A', 'B', 'D'], '有效学习习惯包括复习、规划和练习。'),
    writingQuestion(state, bankId, 'For this part, you are allowed 30 minutes to write a short essay on online learning.'),
  ]
  state.questions.push(...questions)
  for (const question of questions) {
    state.correctLabelsByQuestionId[question.id] = question.options.filter((option) => option.correct).map((option) => option.label)
  }
  refreshBankCounts(state)
}

function choiceQuestion(
  state: DemoState,
  bankId: number,
  type: 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE',
  stem: string,
  labels: string[],
  correctLabels: string[],
  analysis: string,
): Question {
  return {
    id: nextId(state),
    bankId,
    bankName: '四级样例题库',
    type,
    stem,
    analysis,
    difficulty: 'EASY',
    status: 'ACTIVE',
    options: labels.map((content, index) => option(state, index, content, correctLabels.includes(String.fromCharCode(65 + index)))),
    attachments: type === 'SINGLE_CHOICE' ? [listeningAttachment(state)] : [],
  }
}

function writingQuestion(state: DemoState, bankId: number, stem: string): Question {
  return {
    id: nextId(state),
    bankId,
    bankName: '四级样例题库',
    type: 'WRITING',
    stem,
    analysis: '写作题需要人工阅卷。',
    difficulty: 'HARD',
    status: 'ACTIVE',
    options: [],
    attachments: [],
  }
}

function option(state: DemoState, index: number, content: string, correct: boolean): QuestionOption {
  return { id: nextId(state), label: String.fromCharCode(65 + index), content, correct, sortOrder: (index + 1) * 10 }
}

function listeningAttachment(state: DemoState): QuestionAttachment {
  return {
    id: nextId(state),
    fileName: '2023-03-cet4-listening.mp3',
    fileUrl: '/local-assets/cet4/2023-03/set-1/2023-03-cet4-listening.mp3',
    mediaType: 'AUDIO',
    sortOrder: 10,
  }
}

function buildExam(state: DemoState) {
  const questions = state.questions
  const paperQuestions = questions.map((question, index) => ({
    questionId: question.id,
    bankId: question.bankId,
    bankName: question.bankName,
    type: question.type,
    stem: question.stem,
    score: question.type === 'WRITING' ? 30 : 5,
    sortOrder: (index + 1) * 10,
  }))
  state.exams.push({
    id: nextId(state),
    title: 'CET-4 四级考试平台演示',
    description: '以四级真题场景作为样例，演示题库、考试、作答、评分和阅卷流程。',
    qualifyScore: 36,
    startTime: '2025-01-01T00:00:00',
    endTime: '2099-12-31T23:59:59',
    durationMinutes: 45,
    timeLimit: true,
    attemptLimit: null,
    examMode: 'STRUCTURED',
    displayMode: 'ALL',
    questionOrderMode: 'FIXED',
    openType: 'PUBLIC',
    departmentIds: [],
    rules: [],
    paperQuestions,
    materials: [
      {
        id: nextId(state),
        title: '听力音频',
        description: '四级听力演示材料',
        fileName: '2023-03-cet4-listening.mp3',
        fileUrl: '/local-assets/cet4/2023-03/set-1/2023-03-cet4-listening.mp3',
        mediaType: 'AUDIO',
        sortOrder: 10,
      },
    ],
    answerCardItems: [],
    status: 'PUBLISHED',
    questionCount: paperQuestions.length,
    totalScore: paperQuestions.reduce((sum, question) => sum + question.score, 0),
  })
}

function seedSubmittedResult(state: DemoState) {
  const exam = state.exams[0]
  const user = state.users.find((item) => item.username === 'zhangsan')
  if (!exam || !user) {
    return
  }
  const sessionQuestions = buildSessionQuestions(state, exam)
  const answers = sessionQuestions.map((question) => ({
    questionId: question.questionId,
    selectedLabels: isManualReviewType(question.type) ? [] : state.correctLabelsByQuestionId[question.questionId] || [],
    answerText: isManualReviewType(question.type) ? 'Online learning gives students more flexible access to resources and requires stronger self-discipline.' : null,
  }))
  const result = gradeSubmission(state, exam, user, sessionQuestions, answers)
  state.results.push({ ...result, gradingStatus: 'PENDING_REVIEW' })
}

export function buildCurrentUser(state: DemoState, user: DemoUser): CurrentUser {
  const permissions = state.roles
    .filter((role) => user.roleIds.includes(role.id))
    .flatMap((role) => role.permissions.map((permission) => permission.code))
  return {
    id: user.id,
    username: user.username,
    displayName: user.displayName,
    mustChangePassword: false,
    roles: user.roles,
    permissions: Array.from(new Set(permissions)),
  }
}

export function buildSessionQuestions(state: DemoState, exam: Exam): ExamQuestion[] {
  return exam.paperQuestions
    .slice()
    .sort((a, b) => a.sortOrder - b.sortOrder)
    .map((paperQuestion) => {
      const source = state.questions.find((question) => question.id === paperQuestion.questionId)
      const answerCardItem = exam.answerCardItems.find((item) => item.questionNo === Math.abs(paperQuestion.questionId))
      return {
        questionId: paperQuestion.questionId,
        type: paperQuestion.type,
        stem: paperQuestion.stem,
        score: paperQuestion.score,
        sortOrder: paperQuestion.sortOrder,
        selectedLabels: [],
        answerText: null,
        attachments: source?.attachments ? clone(source.attachments) : [],
        options: source?.options.map(({ id, label, content, sortOrder }) => ({ id, label, content, sortOrder }))
          || answerCardItem?.optionLabels.map((label, index) => ({ id: paperQuestion.questionId * 100 - index, label, content: label, sortOrder: (index + 1) * 10 }))
          || [],
      }
    })
}

export function gradeSubmission(
  state: DemoState,
  exam: Exam,
  user: DemoUser,
  questions: ExamQuestion[],
  answers: Array<{ questionId: number; selectedLabels?: string[]; answerText?: string | null }>,
): ExamResultDetail {
  const answerMap = new Map(answers.map((answer) => [answer.questionId, answer]))
  let objectiveScore = 0
  let correctCount = 0
  const resultQuestions: ExamResultQuestion[] = questions.map((question) => {
    const answer = answerMap.get(question.questionId)
    const selectedLabels = answer?.selectedLabels || []
    const answerText = answer?.answerText || null
    const correctLabels = state.correctLabelsByQuestionId[question.questionId] || []
    const correct = isManualReviewType(question.type) ? null : sameLabels(selectedLabels, correctLabels)
    const obtainedScore = correct ? question.score : 0
    if (correct) {
      objectiveScore += obtainedScore
      correctCount += 1
    }
    return {
      ...question,
      analysis: state.questions.find((item) => item.id === question.questionId)?.analysis || null,
      obtainedScore,
      selectedLabels,
      answerText,
      correctLabels,
      correct,
      reviewComment: null,
      reviewerName: null,
      reviewedAt: null,
    }
  })
  const hasManualReview = resultQuestions.some((question) => isManualReviewType(question.type))
  return {
    id: nextId(state),
    attemptId: nextId(state),
    examId: exam.id,
    examTitle: exam.title,
    userId: user.id,
    username: user.username,
    userName: user.displayName,
    departmentName: user.departmentName,
    totalScore: exam.totalScore,
    obtainedScore: objectiveScore,
    objectiveScore,
    subjectiveScore: 0,
    correctCount,
    questionCount: questions.length,
    gradingStatus: hasManualReview ? 'PENDING_REVIEW' : 'FINAL',
    passed: !hasManualReview && objectiveScore >= exam.qualifyScore,
    submittedAt: nowIso(),
    questions: resultQuestions,
  }
}

function sameLabels(left: string[], right: string[]) {
  if (left.length !== right.length) {
    return false
  }
  const leftSorted = [...left].sort()
  const rightSorted = [...right].sort()
  return leftSorted.every((label, index) => label === rightSorted[index])
}

export function updateResultTotals(result: ExamResultDetail, qualifyScore: number) {
  const objectiveScore = result.questions.filter((question) => !isManualReviewType(question.type)).reduce((sum, question) => sum + question.obtainedScore, 0)
  const subjectiveScore = result.questions.filter((question) => isManualReviewType(question.type)).reduce((sum, question) => sum + question.obtainedScore, 0)
  result.objectiveScore = objectiveScore
  result.subjectiveScore = subjectiveScore
  result.obtainedScore = objectiveScore + subjectiveScore
  result.correctCount = result.questions.filter((question) => question.correct).length
  result.passed = result.gradingStatus === 'FINAL' && result.obtainedScore >= qualifyScore
}

export function refreshBankCounts(state: DemoState) {
  for (const bank of state.banks) {
    const questions = state.questions.filter((question) => question.bankId === bank.id)
    bank.questionCount = questions.length
    bank.singleChoiceCount = questions.filter((question) => question.type === 'SINGLE_CHOICE').length
    bank.multipleChoiceCount = questions.filter((question) => question.type === 'MULTIPLE_CHOICE').length
    bank.writingCount = questions.filter((question) => isManualReviewType(question.type)).length
  }
}

export function currentDemoState() {
  return demoState
}

export { clone, nextId, nowIso }

const demoState = seedState()
