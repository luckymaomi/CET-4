import { expect, test } from '@playwright/test'

async function loginDemo(page: import('@playwright/test').Page) {
  await page.goto('/CET-4/login', { waitUntil: 'domcontentloaded' })
  await page.getByLabel('账号').fill('admin')
  await page.getByLabel('密码').fill('password')
  await page.getByRole('button', { name: '进入平台' }).click()
  await expect(page).toHaveURL(/\/CET-4\/dashboard/)
  await expect(page.getByRole('menuitem', { name: '题库管理' })).toBeVisible()
  await expect.poll(() => page.evaluate(() => window.localStorage.getItem('kaoshi.accessToken'))).toBeNull()
  await expect.poll(() => page.evaluate(() => window.localStorage.getItem('kaoshi.currentUser'))).toBeNull()
}

test('GitHub Pages demo 使用同源 UI 和内存数据完成核心体验', async ({ page }) => {
  await loginDemo(page)

  await page.getByRole('menuitem', { name: '题库管理' }).click()
  await expect(page.getByRole('heading', { name: '题库管理' })).toBeVisible()
  await page.locator('.tree-pane').getByText('四级样例题库').click()
  await expect(page.locator('.selected-bank')).toContainText('四级样例题库')
  await expect(page.locator('.selected-bank').getByRole('button', { name: '题组结构' })).toHaveCount(0)

  await page.getByRole('menuitem', { name: '考试中心' }).click()
  await expect(page.getByRole('heading', { name: '考试中心' })).toBeVisible()
  await page.getByRole('button', { name: '准备考试' }).first().click()
  await expect(page.getByRole('button', { name: '开始考试' })).toBeVisible()
  await page.getByRole('button', { name: '开始考试' }).click()
  await expect(page.getByRole('button', { name: '提交试卷' }).first()).toBeVisible()
  await expect(page.locator('.question-media audio').first()).toHaveAttribute('src', /\/CET-4\/local-assets\/cet4\/2023-03\/set-1\/2023-03-cet4-listening\.mp3/)
  await page.locator('.question-panel textarea').first().fill('This is a demo answer for the CET4 writing task.')
  await page.locator('.answer-options .el-radio').first().click()
  await page.getByRole('button', { name: '提交试卷' }).first().click()
  await page.getByRole('dialog', { name: '确认提交' }).getByRole('button', { name: '提交' }).click()
  await expect(page.getByRole('heading', { name: 'CET-4 四级考试平台演示' })).toBeVisible()
  await expect(page.getByText('待阅卷', { exact: true }).first()).toBeVisible()

  await page.locator('.menu .el-menu-item').filter({ hasText: /^考试管理$/ }).click()
  await page.locator('.data-table').getByRole('button', { name: '成绩' }).first().click()
  await expect(page.getByText('考试人数')).toBeVisible()
  await page.getByRole('button', { name: '查看详情' }).first().click()
  await expect(page.getByRole('heading', { name: 'CET-4 四级考试平台演示' })).toBeVisible()
  await page.locator('.writing-review-form').first().getByRole('spinbutton').fill('80')
  await page.locator('.writing-review-form').first().getByRole('button', { name: '保存评分' }).click()
  await expect(page.getByText('评分已保存')).toBeVisible()

  await page.reload({ waitUntil: 'domcontentloaded' })
  await expect(page).toHaveURL(/\/CET-4\/login/)
  await expect.poll(() => page.evaluate(() => window.localStorage.getItem('kaoshi.accessToken'))).toBeNull()
})
