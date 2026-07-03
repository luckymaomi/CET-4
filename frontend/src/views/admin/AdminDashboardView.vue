<template>
  <div class="dashboard">
    <section class="dashboard-hero">
      <div>
        <p class="eyebrow">控制台</p>
        <h1>考试管理工作台</h1>
      </div>
      <el-tag effect="plain" type="success">已登录</el-tag>
    </section>

    <section class="dashboard-grid">
      <article v-for="module in modules" :key="module.title" class="dashboard-module">
        <div class="dashboard-module__head">
          <component :is="module.icon" />
          <h2>{{ module.title }}</h2>
        </div>
        <p>{{ module.summary }}</p>
        <div class="dashboard-actions">
          <el-button v-for="action in module.actions" :key="action.path" text type="primary" @click="router.push(action.path)">
            {{ action.label }}
          </el-button>
        </div>
      </article>
    </section>

    <section class="workflow-panel">
      <h2>常用工作流</h2>
      <ol>
        <li v-for="item in workflows" :key="item">{{ item }}</li>
      </ol>
    </section>

    <section class="metric-grid">
      <article class="metric">
        <span>当前用户</span>
        <strong>{{ auth.user?.displayName }}</strong>
      </article>
      <article class="metric">
        <span>当前角色</span>
        <strong>{{ auth.user?.roles.join(', ') }}</strong>
      </article>
      <article class="metric">
        <span>可用权限</span>
        <strong>{{ auth.permissions.length }}</strong>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Collection, EditPen, Setting } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const modules = [
  {
    title: '在线考试',
    summary: '考生从这里查看可参加考试、进入准备页、开始作答并查看自己的成绩。',
    icon: EditPen,
    actions: [
      { label: '考试中心', path: '/my/exam' },
      { label: '我的成绩', path: '/my/exam/records' },
    ],
  },
  {
    title: '考试管理',
    summary: '考务人员在题库工作台维护题库和试题，再配置考试并发布。',
    icon: Collection,
    actions: [
      { label: '题库管理', path: '/exam/repo' },
      { label: '考试管理', path: '/exam/manage' },
    ],
  },
  {
    title: '系统管理',
    summary: '管理员先维护角色权限，再维护部门结构，最后维护用户归属。',
    icon: Setting,
    actions: [
      { label: '角色管理', path: '/sys/roles' },
      { label: '部门管理', path: '/sys/departments' },
      { label: '用户管理', path: '/sys/users' },
    ],
  },
]

const workflows = [
  '先在系统管理维护角色权限、部门结构和用户归属。',
  '再在题库管理维护题库和试题，富媒体附件在试题编辑页上传或填写 URL。',
  '最后在考试管理按题库规则组卷，配置考试时长、可考次数、题目显示方式和开放范围。',
]
</script>
