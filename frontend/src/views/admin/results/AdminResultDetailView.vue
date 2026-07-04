<template>
  <section class="admin-page" v-loading="loading">
    <header class="admin-page__header">
      <div>
        <h1>{{ result?.examTitle || '成绩详情' }}</h1>
        <p v-if="result">用户 ID：{{ result.userId }}，提交时间：{{ formatDateTime(result.submittedAt) }}</p>
        <p v-else>查看考生答案、正确答案、得分和解析。</p>
      </div>
      <el-button @click="router.push({ name: 'admin-exams' })">返回考试管理</el-button>
    </header>

    <ExamResultReview v-if="result" :result="result" reviewable @updated="result = $event" />
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'

import { fetchAdminResultDetail, type ExamResultDetail } from '@/api/exam-business'
import ExamResultReview from '@/components/exam/ExamResultReview.vue'
import { formatDateTime } from '@/utils/datetime'

const route = useRoute()
const router = useRouter()
const result = ref<ExamResultDetail | null>(null)
const loading = ref(false)

onMounted(loadResult)

async function loadResult() {
  const resultId = Number(route.params.resultId)
  if (!Number.isFinite(resultId)) {
    ElMessage.error('成绩不存在')
    await router.replace({ name: 'admin-exams' })
    return
  }
  loading.value = true
  try {
    result.value = await fetchAdminResultDetail(resultId)
  } finally {
    loading.value = false
  }
}
</script>
