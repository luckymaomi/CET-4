<template>
  <section class="admin-page" v-loading="loading">
    <header class="admin-page__header">
      <div>
        <h1>{{ exam?.title || '准备考试' }}</h1>
        <p>{{ exam ? `${exam.questionCount} 题 / ${exam.totalScore} 分` : '确认考试信息后开始作答。' }}</p>
      </div>
      <div class="header-actions">
        <el-button @click="router.push({ name: 'exam-home' })">返回考试中心</el-button>
        <el-button type="primary" :disabled="!exam" @click="start">开始考试</el-button>
      </div>
    </header>

    <section v-if="exam" class="prepare-panel">
      <el-alert title="点击开始考试后将进入在线作答，请确认网络稳定并诚信考试。" type="warning" show-icon />

      <section class="metric-grid">
        <article class="metric">
          <span>考试时长</span>
          <strong>{{ exam.durationMinutes }} 分钟</strong>
        </article>
        <article class="metric">
          <span>开始时间</span>
          <strong>{{ formatDateTime(exam.startTime) }}</strong>
        </article>
        <article class="metric">
          <span>截止时间</span>
          <strong>{{ formatDateTime(exam.endTime) }}</strong>
        </article>
      </section>

      <article class="question-panel">
        <div class="section-title">
          <h2>考试说明</h2>
        </div>
        <p class="prepare-description">{{ exam.description || '本考试提交后答案将锁定，成绩会进入成绩归档。' }}</p>
      </article>
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'

import { fetchExamTasks, type Exam } from '@/api/exam-business'
import { formatDateTime } from '@/utils/datetime'

const route = useRoute()
const router = useRouter()
const exam = ref<Exam | null>(null)
const loading = ref(false)

onMounted(loadExam)

async function loadExam() {
  loading.value = true
  try {
    const examId = Number(route.params.examId)
    const tasks = await fetchExamTasks()
    exam.value = tasks.find((item) => item.id === examId) || null
    if (!exam.value) {
      ElMessage.error('考试不存在或未发布')
      await router.replace({ name: 'exam-home' })
    }
  } finally {
    loading.value = false
  }
}

async function start() {
  if (!exam.value) {
    return
  }
  await router.push({ name: 'exam-session', params: { examId: exam.value.id } })
}
</script>

<style scoped>
.prepare-panel {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.prepare-description {
  margin: 0;
  color: var(--ks-text-muted);
  line-height: 1.8;
  overflow-wrap: anywhere;
}
</style>
