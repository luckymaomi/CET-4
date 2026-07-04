import { describe, expect, it } from 'vitest'

import { resolveResourceUrl } from '@/utils/resource-url'

describe('resolveResourceUrl', () => {
  it('keeps external object and relative urls unchanged', () => {
    expect(resolveResourceUrl('https://example.com/audio.mp3', '/CET-4/')).toBe('https://example.com/audio.mp3')
    expect(resolveResourceUrl('blob:https://example.com/id', '/CET-4/')).toBe('blob:https://example.com/id')
    expect(resolveResourceUrl('local-file.mp3', '/CET-4/')).toBe('local-file.mp3')
  })

  it('prefixes root relative local assets with the app base path', () => {
    expect(resolveResourceUrl('/local-assets/cet4/listening.mp3', '/CET-4/')).toBe('/CET-4/local-assets/cet4/listening.mp3')
  })

  it('does not duplicate an existing app base path', () => {
    expect(resolveResourceUrl('/CET-4/local-assets/cet4/listening.mp3', '/CET-4/')).toBe('/CET-4/local-assets/cet4/listening.mp3')
  })
})
