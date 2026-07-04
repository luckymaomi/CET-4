const absoluteUrlPattern = /^[a-z][a-z\d+.-]*:/i

export function resolveResourceUrl(url: string, base = import.meta.env.BASE_URL) {
  if (!url || absoluteUrlPattern.test(url) || url.startsWith('//') || !url.startsWith('/')) {
    return url
  }

  const normalizedBase = base.endsWith('/') ? base : `${base}/`
  if (normalizedBase === '/' || url.startsWith(normalizedBase)) {
    return url
  }

  return `${normalizedBase.slice(0, -1)}${url}`
}
