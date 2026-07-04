import { computed, reactive, ref } from 'vue'

export interface AnswerSnapshotSaverOptions {
  debounceMs?: number
  saveSnapshot: () => Promise<void>
  canSave?: () => boolean
  onAutoSaveError?: () => void
}

export function useAnswerSnapshotSaver(options: AnswerSnapshotSaverOptions) {
  const debounceMs = options.debounceMs ?? 300
  const savingCount = ref(0)
  const lastSavedAt = ref<Date | null>(null)
  const saveFailed = ref(false)
  const dirtyQuestionIds = reactive(new Set<number>())
  let saveTimer: number | undefined
  let saveChain: Promise<void> = Promise.resolve()
  let saveVersion = 0

  const hasPendingSave = computed(() => dirtyQuestionIds.size > 0 || savingCount.value > 0)
  const saveStatusText = computed(() => {
    if (saveFailed.value) {
      return '答案保存失败'
    }
    if (savingCount.value > 0) {
      return '答案保存中'
    }
    if (dirtyQuestionIds.size > 0) {
      return '答案待保存'
    }
    return lastSavedAt.value ? '答案已保存' : '答案待保存'
  })

  function markDirty(questionIds: number | number[]) {
    const ids = Array.isArray(questionIds) ? questionIds : [questionIds]
    for (const questionId of ids) {
      dirtyQuestionIds.add(questionId)
    }
    saveVersion += 1
    saveFailed.value = false
  }

  function scheduleSave(questionId: number) {
    if (!canSave()) {
      return
    }
    markDirty(questionId)
    clearScheduledSave()
    saveTimer = window.setTimeout(() => {
      saveTimer = undefined
      void queueSave().catch(() => {
        options.onAutoSaveError?.()
      })
    }, debounceMs)
  }

  function saveImmediately(questionId: number) {
    if (!canSave()) {
      return
    }
    markDirty(questionId)
    clearScheduledSave()
    void queueSave().catch(() => {
      options.onAutoSaveError?.()
    })
  }

  async function flushSaves(questionIds?: number[]) {
    if (!canSave()) {
      return
    }
    clearScheduledSave()
    if (questionIds) {
      markDirty(questionIds)
    }
    await queueSave()
  }

  function clearScheduledSave() {
    if (saveTimer) {
      window.clearTimeout(saveTimer)
      saveTimer = undefined
    }
  }

  function queueSave() {
    const version = saveVersion
    const currentSave = saveChain.catch(() => undefined).then(() => persistSnapshot(version))
    saveChain = currentSave
    return currentSave
  }

  async function persistSnapshot(version: number) {
    if (!canSave()) {
      return
    }
    savingCount.value += 1
    try {
      await options.saveSnapshot()
      saveFailed.value = false
      if (saveVersion === version) {
        dirtyQuestionIds.clear()
      }
      lastSavedAt.value = new Date()
    } catch (error) {
      saveFailed.value = true
      throw error
    } finally {
      savingCount.value = Math.max(0, savingCount.value - 1)
    }
  }

  function canSave() {
    return options.canSave?.() ?? true
  }

  return {
    savingCount,
    lastSavedAt,
    saveFailed,
    hasPendingSave,
    saveStatusText,
    markDirty,
    scheduleSave,
    saveImmediately,
    flushSaves,
    clearScheduledSave,
  }
}
