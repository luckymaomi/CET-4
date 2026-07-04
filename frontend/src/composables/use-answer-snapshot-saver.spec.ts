import { describe, expect, it, vi } from 'vitest'

import { useAnswerSnapshotSaver } from './use-answer-snapshot-saver'

describe('useAnswerSnapshotSaver', () => {
  it('debounces automatic snapshot saves', async () => {
    vi.useFakeTimers()
    const saveSnapshot = vi.fn().mockResolvedValue(undefined)
    const saver = useAnswerSnapshotSaver({ saveSnapshot })

    saver.scheduleSave(1)
    saver.scheduleSave(2)

    expect(saver.saveStatusText.value).toBe('答案待保存')
    expect(saveSnapshot).not.toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(300)

    expect(saveSnapshot).toHaveBeenCalledTimes(1)
    expect(saver.hasPendingSave.value).toBe(false)
    expect(saver.saveStatusText.value).toBe('答案已保存')
    vi.useRealTimers()
  })

  it('flushes immediately and keeps dirty state after a failed save', async () => {
    const saveSnapshot = vi.fn()
      .mockRejectedValueOnce(new Error('network'))
      .mockResolvedValueOnce(undefined)
    const saver = useAnswerSnapshotSaver({ saveSnapshot })

    await expect(saver.flushSaves([1, 2])).rejects.toThrow('network')
    expect(saver.hasPendingSave.value).toBe(true)
    expect(saver.saveStatusText.value).toBe('答案保存失败')

    await saver.flushSaves([1, 2])

    expect(saveSnapshot).toHaveBeenCalledTimes(2)
    expect(saver.hasPendingSave.value).toBe(false)
    expect(saver.saveStatusText.value).toBe('答案已保存')
  })

  it('does not save when canSave returns false', async () => {
    const saveSnapshot = vi.fn().mockResolvedValue(undefined)
    const saver = useAnswerSnapshotSaver({ saveSnapshot, canSave: () => false })

    saver.scheduleSave(1)
    await saver.flushSaves([1])

    expect(saveSnapshot).not.toHaveBeenCalled()
    expect(saver.hasPendingSave.value).toBe(false)
  })
})
