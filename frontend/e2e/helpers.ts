import { expect, type Locator, type Page } from '@playwright/test'

export async function login(page: Page) {
  await page.goto('/login', { waitUntil: 'domcontentloaded' })
  if (await page.getByRole('menuitem', { name: '角色管理' }).isVisible({ timeout: 1000 }).catch(() => false)) {
    await expect
      .poll(() => page.evaluate(() => window.localStorage.getItem('kaoshi.accessToken')))
      .toBeTruthy()
    return
  }
  await expect(page.getByLabel('账号')).toBeVisible()
  await page.getByLabel('账号').fill('admin')
  await page.getByLabel('密码').fill('password')
  const loginResponse = page.waitForResponse((response) => {
    return response.url().includes('/api/auth/login') && response.request().method() === 'POST'
  })
  await page.getByRole('button', { name: '进入平台' }).click()
  await expect((await loginResponse).ok()).toBe(true)
  await expect(page).toHaveURL(/\/dashboard/)
  await expect(page.getByRole('menuitem', { name: '角色管理' })).toBeVisible()
  await expect
    .poll(() => page.evaluate(() => window.localStorage.getItem('kaoshi.accessToken')))
    .toBeTruthy()
}

export async function chooseSelectOption(page: Page, root: Locator, label: string, option: string) {
  await root.locator('.el-form-item').filter({ hasText: label }).locator('.el-select').click()
  await page.getByRole('option', { name: option }).click()
  await page.keyboard.press('Escape')
}

export async function downloadByButton(page: Page, name: string) {
  const downloadPromise = page.waitForEvent('download')
  await page.getByRole('button', { name }).click()
  const download = await downloadPromise
  const path = await download.path()
  expect(path).toBeTruthy()
  return path as string
}

export async function setNumberInput(input: Locator, value: string) {
  await input.fill('')
  await input.fill(value)
  await input.press('Tab')
}

export function collectConsoleIssues(page: Page) {
  const issues: string[] = []
  page.on('console', (message) => {
    if (!['warning', 'error'].includes(message.type())) {
      return
    }
    const text = message.text()
    if (isIgnoredBrowserNoise(text)) {
      return
    }
    issues.push(`${message.type()}: ${text}`)
  })
  page.on('pageerror', (error) => {
    if (isIgnoredBrowserNoise(error.message)) {
      return
    }
    issues.push(`pageerror: ${error.message}`)
  })
  return issues
}

function isIgnoredBrowserNoise(text: string) {
  return text.includes('ResizeObserver loop')
}
