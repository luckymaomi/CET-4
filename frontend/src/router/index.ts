import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { title: '控制台' },
    children: [
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        component: () => import('@/views/admin/AdminDashboardView.vue'),
        meta: { title: '工作台' },
      },
      {
        path: 'sys/users',
        name: 'admin-users',
        component: () => import('@/views/admin/users/UsersView.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'sys/roles',
        name: 'admin-roles',
        component: () => import('@/views/admin/roles/RolesView.vue'),
        meta: { title: '角色管理' },
      },
      {
        path: 'sys/departments',
        name: 'admin-departments',
        component: () => import('@/views/admin/departments/DepartmentsView.vue'),
        meta: { title: '部门管理' },
      },
      {
        path: 'exam/repo',
        name: 'admin-question-banks',
        component: () => import('@/views/admin/question-banks/QuestionBanksView.vue'),
        meta: { title: '题库管理' },
      },
      {
        path: 'exam/qu',
        name: 'admin-questions',
        component: () => import('@/views/admin/questions/QuestionsView.vue'),
        meta: { title: '试题管理' },
      },
      {
        path: 'exam/manage',
        name: 'admin-exams',
        component: () => import('@/views/admin/exams/ExamsView.vue'),
        meta: { title: '考试管理' },
      },
      {
        path: 'exam/manage/records/:resultId',
        name: 'admin-result-detail',
        component: () => import('@/views/admin/results/AdminResultDetailView.vue'),
        meta: { title: '成绩详情', activeMenu: '/exam/manage' },
      },
      {
        path: 'my/exam',
        name: 'exam-home',
        component: () => import('@/views/exam/ExamHomeView.vue'),
        meta: { title: '在线考试' },
      },
      {
        path: 'my/exam/records',
        name: 'exam-my-results',
        component: () => import('@/views/exam/MyResultsView.vue'),
        meta: { title: '我的成绩' },
      },
      {
        path: 'my/exam/prepare/:examId',
        name: 'exam-prepare',
        component: () => import('@/views/exam/ExamPrepareView.vue'),
        meta: { title: '准备考试', activeMenu: '/my/exam' },
      },
      {
        path: 'my/exam/result/:resultId',
        name: 'exam-result',
        component: () => import('@/views/exam/ExamResultView.vue'),
        meta: { title: '考试结果', activeMenu: '/my/exam/records' },
      },
      {
        path: 'my/exam/start/:examId',
        name: 'exam-session',
        component: () => import('@/views/exam/ExamSessionView.vue'),
        meta: { title: '在线作答', activeMenu: '/my/exam' },
      },
    ],
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/change-password',
    name: 'change-password',
    component: () => import('@/views/auth/ChangePasswordView.vue'),
    meta: { title: '修改密码' },
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/error/ForbiddenView.vue'),
    meta: { public: true, title: '无权限' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: { public: true, title: '页面不存在' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  document.title = `${String(to.meta.title || 'kaoshi')} - kaoshi`
  const auth = useAuthStore()
  if (to.meta.public) {
    if (to.name === 'login' && auth.isAuthenticated) {
      if (auth.user?.mustChangePassword) {
        return { name: 'change-password' }
      }
      return { name: 'admin-dashboard' }
    }
    return true
  }
  if (!auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (!auth.user) {
    await auth.loadCurrentUser()
  }
  if (auth.user?.mustChangePassword && to.name !== 'change-password') {
    return { name: 'change-password' }
  }
  if (!auth.user?.mustChangePassword && to.name === 'change-password') {
    return { name: 'admin-dashboard' }
  }
  return true
})

router.onError((error, to) => {
  console.error('[kaoshi 路由加载失败]', { path: to.fullPath, error })
})

export default router

