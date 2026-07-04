<template>
  <section class="metric-grid">
    <article class="metric">
      <span>得分</span>
      <strong>{{ result.obtainedScore }}</strong>
    </article>
    <article class="metric">
      <span>总分</span>
      <strong>{{ result.totalScore }}</strong>
    </article>
    <article class="metric">
      <span>正确题数</span>
      <strong>{{ result.correctCount }} / {{ result.questionCount }}</strong>
    </article>
    <article class="metric">
      <span>客观题分</span>
      <strong>{{ result.objectiveScore }}</strong>
    </article>
    <article class="metric">
      <span>主观题分</span>
      <strong>{{ result.subjectiveScore }}</strong>
    </article>
    <article class="metric">
      <span>阅卷状态</span>
      <strong>{{ result.gradingStatus === 'FINAL' ? '已出分' : '待阅卷' }}</strong>
    </article>
  </section>
</template>

<script setup lang="ts">
import type { ExamResultDetail } from '@/api/exam-business'

defineProps<{
  result: ExamResultDetail
}>()
</script>
