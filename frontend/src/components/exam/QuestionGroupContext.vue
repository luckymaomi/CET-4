<template>
  <section class="question-group-context">
    <div class="question-group-context__title">
      <span v-if="sectionTitle">{{ sectionTitle }}</span>
      <strong>{{ title }}</strong>
    </div>
    <p v-if="direction" class="question-group-context__direction">{{ direction }}</p>
    <pre v-if="material" class="question-group-context__material">{{ material }}</pre>
    <div v-if="sharedOptions.length" class="question-group-context__options">
      <div v-for="option in sharedOptions" :key="option.id" class="question-group-context__option">
        <strong>{{ option.label }}</strong>
        <span>{{ option.content }}</span>
      </div>
    </div>
    <div v-if="attachments.length" class="question-group-context__media">
      <template v-for="attachment in attachments" :key="attachment.id">
        <img v-if="attachment.mediaType === 'IMAGE'" :src="resolveResourceUrl(attachment.fileUrl)" :alt="attachment.fileName" />
        <audio v-else-if="attachment.mediaType === 'AUDIO'" :src="resolveResourceUrl(attachment.fileUrl)" controls />
        <el-link v-else :href="resolveResourceUrl(attachment.fileUrl)" target="_blank">{{ attachment.fileName }}</el-link>
      </template>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ExamQuestionOption, QuestionAttachment } from '@/api/exam-business'
import { resolveResourceUrl } from '@/utils/resource-url'

withDefaults(defineProps<{
  sectionTitle: string
  title: string
  direction: string | null
  material: string | null
  attachments: QuestionAttachment[]
  sharedOptions?: ExamQuestionOption[]
}>(), {
  sharedOptions: () => [],
})
</script>

<style scoped>
.question-group-context {
  display: grid;
  gap: 12px;
  padding: 18px 0 8px;
}

.question-group-context__title {
  display: grid;
  gap: 4px;
}

.question-group-context__title span {
  color: var(--ks-text-muted);
  font-size: 13px;
}

.question-group-context__title strong {
  color: var(--ks-text);
  font-size: 18px;
}

.question-group-context__direction {
  margin: 0;
  color: var(--ks-text-muted);
  line-height: 1.7;
}

.question-group-context__material {
  max-height: 460px;
  margin: 0;
  padding: 14px;
  overflow: auto;
  border: 1px solid var(--ks-border);
  border-radius: var(--ks-radius);
  background: var(--ks-panel-muted);
  color: var(--ks-text);
  font-family: inherit;
  line-height: 1.7;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.question-group-context__media {
  display: grid;
  gap: 12px;
}

.question-group-context__options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--ks-border);
  border-radius: var(--ks-radius);
  background: var(--ks-panel-muted);
}

.question-group-context__option {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 8px;
  align-items: start;
  min-width: 0;
  color: var(--ks-text);
  line-height: 1.5;
}

.question-group-context__option strong {
  color: var(--ks-primary);
}

.question-group-context__option span {
  min-width: 0;
  overflow-wrap: anywhere;
}

.question-group-context__media img {
  width: min(100%, 720px);
  max-height: 360px;
  object-fit: contain;
  border: 1px solid var(--ks-border);
  border-radius: var(--ks-radius);
  background: var(--ks-panel-muted);
}

.question-group-context__media audio {
  width: min(100%, 720px);
}
</style>
