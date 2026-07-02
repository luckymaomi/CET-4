<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1>考试中心</h1>
      </div>
      <div class="header-actions">
        <el-button @click="router.push({ name: 'exam-my-results' })">我的成绩</el-button>
        <el-button :icon="Refresh" @click="loadTasks">刷新</el-button>
      </div>
    </header>

    <el-table v-loading="loading" :data="tasks" class="data-table" border>
      <el-table-column prop="title" label="考试名称" min-width="180" />
      <el-table-column label="题量/总分" width="130">
        <template #default="{ row }: { row: Exam }">{{ row.questionCount }} 题 / {{ row.totalScore }} 分</template>
      </el-table-column>
      <el-table-column label="开始时间" width="180">
        <template #default="{ row }: { row: Exam }">{{ formatDateTime(row.startTime) }}</template>
      </el-table-column>
      <el-table-column label="截止时间" width="180">
        <template #default="{ row }: { row: Exam }">{{ formatDateTime(row.endTime) }}</template>
      </el-table-column>
      <el-table-column prop="durationMinutes" label="限时" width="90" />
      <el-table-column fixed="right" label="操作" width="120">
        <template #default="{ row }: { row: Exam }">
          <el-button type="primary" link @click="router.push({ name: 'exam-prepare', params: { examId: row.id } })">
            准备考试
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

import { fetchExamTasks, type Exam } from '@/api/exam-business'
import { formatDateTime } from '@/utils/datetime'

const router = useRouter()
const tasks = ref<Exam[]>([])
const loading = ref(false)

onMounted(loadTasks)

async function loadTasks() {
  loading.value = true
  try {
    tasks.value = await fetchExamTasks()
  } finally {
    loading.value = false
  }
}
</script>
