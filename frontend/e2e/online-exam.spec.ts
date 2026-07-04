import { expect, test, type Page } from '@playwright/test'

import { collectConsoleIssues, login } from './helpers'

const CET4_EXAM = '2015年12月英语四级真题第一卷'
const CET4_AUDIO = '201512cet4-01.mp3'

test.describe('在线考试', () => {
  function cet4ExamRow(page: Page) {
    return page.getByRole('row').filter({ hasText: CET4_EXAM })
  }

  test('考试中心支持进入 CET4 准备页并返回', async ({ page }) => {
    const consoleIssues = collectConsoleIssues(page)
    await login(page)

    await page.getByRole('menuitem', { name: '考试中心' }).click()
    await expect(page.getByRole('heading', { name: '考试中心' })).toBeVisible()
    await cet4ExamRow(page).getByRole('button', { name: '准备考试' }).click()
    await expect(page.getByRole('heading', { name: CET4_EXAM })).toBeVisible()
    await page.getByRole('button', { name: '返回考试中心' }).click()
    await expect(page.getByRole('heading', { name: '考试中心' })).toBeVisible()

    expect(consoleIssues).toEqual([])
  })

  test('CET4 在线作答支持写作保存读回、音频题作答、提交待阅卷和人工阅卷锁定', async ({ page }) => {
    const consoleIssues = collectConsoleIssues(page)
    await login(page)

    await page.getByRole('menuitem', { name: '考试中心' }).click()
    await expect(page.getByRole('heading', { name: '考试中心' })).toBeVisible()
    await cet4ExamRow(page).getByRole('button', { name: '准备考试' }).click()
    await page.getByRole('button', { name: '开始考试' }).click()
    await expect(page.getByText('答题卡')).toBeVisible()
    await expect(page.getByText('写作题').first()).toBeVisible()
    await expect(page.getByText('Listening is more important than talking')).toBeVisible()

    const writingAnswer = 'Listening matters because it helps people understand others before responding. Careful listening builds trust and improves communication.'
    const writingSaveResponse = page.waitForResponse((response) => {
      return response.url().includes('/api/exam/1/answers')
        && response.request().method() === 'POST'
        && Boolean(response.request().postData()?.includes('Careful listening'))
    })
    await page.getByPlaceholder('请输入写作答案').fill(writingAnswer)
    await page.getByPlaceholder('请输入写作答案').blur()
    await expect((await writingSaveResponse).ok()).toBe(true)

    page.once('dialog', (dialog) => {
      void dialog.accept()
    })
    await page.reload()
    await expect(page.getByText('答题卡')).toBeVisible()
    await expect(page.getByPlaceholder('请输入写作答案')).toHaveValue(writingAnswer)

    await page.locator('.answer-card__item').filter({ hasText: /^2$/ }).click()
    await expect(page.locator(`audio.question-media__audio[src$="${CET4_AUDIO}"]`).first()).toBeVisible()
    await page.getByText('C. They enjoyed the movie on space exploration.').click()
    await expect(page.getByText('答案已保存')).toBeVisible()

    await page.locator('.exam-actions').getByRole('button', { name: '提交试卷' }).click()
    await page.getByRole('button', { name: '提交', exact: true }).click()

    await expect(page.getByText('本次考试已经提交，答案已锁定。')).toBeVisible()
    await expect(page.getByRole('heading', { name: CET4_EXAM })).toBeVisible()
    await expect(page.getByText('阅卷状态').locator('..')).toContainText('待阅卷')
    await expect(page.getByText(writingAnswer)).toBeVisible()
    await expect(page.getByText('正确答案：C').first()).toBeVisible()

    await page.getByRole('menuitem', { name: '我的成绩' }).click()
    await expect(page.getByRole('heading', { name: '我的成绩' })).toBeVisible()
    await page.getByRole('button', { name: '查看详情' }).first().click()
    await expect(page.getByText(writingAnswer)).toBeVisible()

    await page.locator('li.el-menu-item').filter({ hasText: /^考试管理$/ }).click()
    await expect(page.locator('.admin-page__header h1', { hasText: '考试管理' })).toBeVisible()
    await page.getByPlaceholder('搜索考试名称').fill(CET4_EXAM)
    await page.getByRole('button', { name: '搜索' }).click()
    await cet4ExamRow(page).getByRole('button', { name: '成绩' }).click()
    const resultDrawer = page.getByRole('dialog', { name: new RegExp(`${CET4_EXAM} - 成绩`) })
    await expect(resultDrawer).toBeVisible()
    await expect(resultDrawer.getByText('待阅卷').first()).toBeVisible()
    await resultDrawer.getByRole('button', { name: '查看详情' }).first().click()
    await expect(page.getByRole('heading', { name: CET4_EXAM })).toBeVisible()
    await expect(page.getByText(writingAnswer)).toBeVisible()

    const resultId = Number(page.url().split('/').pop())
    expect(Number.isFinite(resultId)).toBe(true)
    const firstReviewForm = page.locator('.writing-review-form').first()
    await firstReviewForm.locator('.el-input-number input').fill('1')
    await firstReviewForm.getByPlaceholder('阅卷评语').fill('CET4 主观题评分 1')
    const firstReviewResponse = page.waitForResponse((response) => {
      return response.url().includes(`/api/admin/results/${resultId}/questions/`)
        && response.request().method() === 'POST'
    })
    await firstReviewForm.getByRole('button', { name: '保存评分' }).click()
    await expect((await firstReviewResponse).ok()).toBe(true)
    await expect(page.getByText('评分已保存').last()).toBeVisible()

    await page.reload()
    await expect(page.getByText('CET4 主观题评分 1')).toBeVisible()
    const token = await page.evaluate(() => window.localStorage.getItem('kaoshi.accessToken'))
    const headers = { Authorization: `Bearer ${token}` }
    const detailResponse = await page.request.get(`/api/admin/results/${resultId}`, { headers })
    expect(detailResponse.ok()).toBeTruthy()
    const resultDetail = (await detailResponse.json()).data
    for (const question of resultDetail.questions.filter((item: { type: string; reviewedAt: string | null }) => item.type === 'WRITING' && !item.reviewedAt)) {
      const response = await page.request.post(`/api/admin/results/${resultId}/questions/${question.questionId}/review`, {
        headers,
        data: {
          score: 1,
          comment: `CET4 主观题评分 ${question.questionId}`,
        },
      })
      expect(response.ok()).toBeTruthy()
    }
    await page.reload()
    await expect(page.getByRole('button', { name: '完成阅卷' })).toBeEnabled()
    await page.getByRole('button', { name: '完成阅卷' }).click()
    await expect(page.getByText('阅卷已完成')).toBeVisible()
    await expect(page.getByText('阅卷状态').locator('..')).toContainText('已出分')
    await expect(page.getByRole('button', { name: '保存评分' })).toHaveCount(0)

    expect(consoleIssues).toEqual([])
  })
})
