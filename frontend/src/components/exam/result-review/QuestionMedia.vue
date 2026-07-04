<template>
  <div v-if="attachments.length" class="question-media">
    <template v-for="attachment in attachments" :key="attachment.id">
      <img
        v-if="attachment.mediaType === 'IMAGE'"
        :src="resolveResourceUrl(attachment.fileUrl)"
        :alt="attachment.fileName"
        class="question-media__image"
      />
      <audio v-else-if="attachment.mediaType === 'AUDIO'" :src="resolveResourceUrl(attachment.fileUrl)" controls class="question-media__audio" />
      <el-link v-else :href="resolveResourceUrl(attachment.fileUrl)" target="_blank">{{ attachment.fileName }}</el-link>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { QuestionAttachment } from '@/api/exam-business'
import { resolveResourceUrl } from '@/utils/resource-url'

defineProps<{
  attachments: QuestionAttachment[]
}>()
</script>

<style scoped>
.question-media {
  display: grid;
  gap: 10px;
  margin: 12px 0;
}

.question-media__image {
  display: block;
  max-width: min(520px, 100%);
  max-height: 280px;
  border: 1px solid var(--ks-border);
  border-radius: var(--ks-radius);
  object-fit: contain;
}

.question-media__audio {
  width: min(520px, 100%);
}
</style>
