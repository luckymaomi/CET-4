<template>
  <el-dialog :model-value="visible" title="试卷预览" width="min(920px, 94vw)" @update:model-value="$emit('update:visible', $event)">
    <div class="paper-preview">
      <article v-for="(question, index) in questions" :key="question.questionId" class="preview-question">
        <header>
          <strong>{{ index + 1 }}. {{ question.stem }}</strong>
          <el-tag effect="plain">{{ questionTypeText(question.type) }} · {{ question.score }} 分</el-tag>
        </header>
        <span class="muted-text">{{ question.bankName }}</span>
      </article>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import type { ExamPaperQuestionForm } from '@/utils/admin-exam-editor'
import { questionTypeText } from '@/utils/question-types'

defineProps<{
  visible: boolean
  questions: ExamPaperQuestionForm[]
}>()

defineEmits<{
  'update:visible': [visible: boolean]
}>()
</script>

<style scoped>
.paper-preview {
  display: grid;
  gap: 12px;
  max-height: min(70vh, 680px);
  overflow: auto;
}

.preview-question {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 12px 0;
  border-bottom: 1px solid var(--ks-border);
}

.preview-question header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.preview-question strong {
  min-width: 0;
  overflow-wrap: anywhere;
}
</style>
