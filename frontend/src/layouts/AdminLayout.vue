<template>
  <el-container class="shell">
    <el-aside class="sidebar" width="232px">
      <div class="brand">
        <span class="brand-mark">
          <el-icon><Reading /></el-icon>
        </span>
        <div>
          <strong>kaoshi</strong>
          <small>考试管理平台</small>
        </div>
      </div>
      <el-menu router :default-active="activeMenu" :default-openeds="defaultOpeneds" class="menu">
        <el-menu-item index="/dashboard">
          <el-icon><Grid /></el-icon>
          <span>控制台</span>
        </el-menu-item>

        <el-sub-menu index="online-exam">
          <template #title>
            <el-icon><EditPen /></el-icon>
            <span>在线考试</span>
          </template>
          <el-menu-item index="/my/exam">考试中心</el-menu-item>
          <el-menu-item index="/my/exam/records">我的成绩</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="exam-management">
          <template #title>
            <el-icon><Collection /></el-icon>
            <span>考试管理</span>
          </template>
          <el-menu-item index="/exam/repo">题库管理</el-menu-item>
          <el-menu-item index="/exam/manage">考试管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="system-management">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/sys/roles">角色管理</el-menu-item>
          <el-menu-item index="/sys/departments">部门管理</el-menu-item>
          <el-menu-item index="/sys/users">用户管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div class="topbar__main">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>控制台</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentSection">{{ currentSection }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <el-dropdown @command="handleCommand">
          <button class="user-button" type="button">
            <el-avatar :size="32">{{ auth.displayName.slice(0, 1) }}</el-avatar>
            <span>{{ auth.displayName }}</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <div class="visited-tabs">
        <button
          v-for="tab in visitedTabs"
          :key="tab.path"
          class="visited-tab"
          :class="{ 'visited-tab--active': tab.path === route.fullPath }"
          type="button"
          @click="router.push(tab.path)"
        >
          <span>{{ tab.title }}</span>
          <el-icon v-if="visitedTabs.length > 1" class="visited-tab__close" @click.stop="closeTab(tab.path)">
            <Close />
          </el-icon>
        </button>
      </div>
      <el-main class="content">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import {
  Close,
  Collection,
  EditPen,
  Grid,
  Reading,
  Setting,
} from '@element-plus/icons-vue'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const activeMenu = computed(() => String(route.meta.activeMenu || route.path))
const defaultOpeneds = ['online-exam', 'exam-management', 'system-management']
const visitedTabs = ref<Array<{ path: string; title: string }>>([])
const currentTitle = computed(() => String(route.meta.title || '管理端'))
const currentSection = computed(() => {
  const path = route.path
  if (path.startsWith('/my/exam')) {
    return '在线考试'
  }
  if (path.startsWith('/exam')) {
    return '考试管理'
  }
  if (path.startsWith('/sys')) {
    return '系统管理'
  }
  return ''
})

watch(
  () => route.fullPath,
  () => {
    if (route.meta.public) {
      return
    }
    const existing = visitedTabs.value.find((tab) => tab.path === route.fullPath)
    if (existing) {
      existing.title = currentTitle.value
      return
    }
    visitedTabs.value.push({ path: route.fullPath, title: currentTitle.value })
  },
  { immediate: true },
)

async function handleCommand(command: string) {
  if (command === 'logout') {
    await auth.logout()
    await router.replace({ name: 'login' })
  }
}

async function closeTab(path: string) {
  const index = visitedTabs.value.findIndex((tab) => tab.path === path)
  if (index < 0) {
    return
  }
  visitedTabs.value.splice(index, 1)
  if (path === route.fullPath) {
    const nextTab = visitedTabs.value[Math.max(0, index - 1)] || visitedTabs.value[0]
    await router.push(nextTab?.path || '/dashboard')
  }
}
</script>

