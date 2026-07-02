import { expect, test, type Page } from '@playwright/test'

import { collectConsoleIssues, login } from './helpers'

test.describe('在线考试', () => {
  function englishExamRow(page: Page) {
    return page.getByRole('row').filter({ hasText: '英语基础模拟考试' })
  }

  test('考试中心支持进入准备页并返回', async ({ page }) => {
    const consoleIssues = collectConsoleIssues(page)
    await login(page)

    await page.getByRole('menuitem', { name: '考试中心' }).click()
    await expect(page.getByRole('heading', { name: '考试中心' })).toBeVisible()
    await englishExamRow(page).getByRole('button', { name: '准备考试' }).click()
    await expect(page.getByRole('heading', { name: '英语基础模拟考试' })).toBeVisible()
    await page.getByRole('button', { name: '返回考试中心' }).click()
    await expect(page.getByRole('heading', { name: '考试中心' })).toBeVisible()

    expect(consoleIssues).toEqual([])
  })

  test('在线作答支持题型提示、富媒体、翻页、提交锁定和成绩详情', async ({ page }) => {
    const consoleIssues = collectConsoleIssues(page)
    await login(page)

    await page.getByRole('menuitem', { name: '考试中心' }).click()
    await expect(page.getByRole('heading', { name: '考试中心' })).toBeVisible()
    await englishExamRow(page).getByRole('button', { name: '准备考试' }).click()
    await page.getByRole('button', { name: '开始考试' }).click()
    await expect(page.getByText('答题卡')).toBeVisible()
    await expect(page.getByText('单选题').first()).toBeVisible()
    await expect(page.locator('audio.question-media__audio[src$="dog-wolf-friendship.mp3"]').first()).toBeVisible()

    await page.getByText('B. He goes to school every day.').click()
    await page.getByRole('button', { name: '下一题' }).click()
    await expect(page.locator('img.question-media__image[src$="improve-card.jpg"]').first()).toBeVisible()
    await expect(page.getByText('单选题').first()).toBeVisible()
    await page.getByText('A. improve').click()
    await page.getByRole('button', { name: '上一题' }).click()
    await expect(page.getByText('Choose the correct sentence.')).toBeVisible()
    await page.getByRole('button', { name: '下一题' }).click()
    await page.getByRole('button', { name: '下一题' }).click()
    await expect(page.locator('img.question-media__image[src$="noun-example.png"]').first()).toBeVisible()
    await expect(page.getByText('多选题').first()).toBeVisible()
    await page.getByText('A. book').click()
    await page.getByText('C. teacher').click()
    await page.getByRole('button', { name: '下一题' }).click()
    await expect(page.locator('img.question-media__image[src$="practice-chart.png"]').first()).toBeVisible()
    await expect(page.locator('audio.question-media__audio[src$="dog-wolf-friendship.mp3"]').first()).toBeVisible()
    await page.getByText('A. The learner practiced reading.').click()
    await page.getByText('B. The learner practiced listening.').click()
    await page.getByRole('article').getByRole('button', { name: '提交试卷' }).click()
    await page.getByRole('button', { name: '提交', exact: true }).click()

    await expect(page.getByText('本次考试已经提交，答案已锁定。')).toBeVisible()
    await expect(page.getByRole('article').filter({ hasText: '得分' }).getByRole('strong')).toHaveText('20')
    await expect(page.getByRole('article').filter({ hasText: '总分' }).getByRole('strong')).toHaveText('20')
    await expect(page.getByText('正确答案：B')).toBeVisible()
    await expect(page.getByText('解析：Subject and verb agreement: He goes to school every day.')).toBeVisible()
    await page.getByRole('menuitem', { name: '我的成绩' }).click()
    await expect(page.getByRole('heading', { name: '我的成绩' })).toBeVisible()
    await page.getByRole('button', { name: '查看详情' }).first().click()
    await expect(page.getByText('正确答案：B')).toBeVisible()

    expect(consoleIssues).toEqual([])
  })
})
