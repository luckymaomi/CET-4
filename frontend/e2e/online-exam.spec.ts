import { expect, test, type Page } from '@playwright/test'

import { collectConsoleIssues, login } from './helpers'

test.describe('在线考试', () => {
  function englishExamRow(page: Page) {
    return page.getByRole('row').filter({ hasText: '英语基础模拟考试' })
  }

  async function ensureCheckboxChecked(page: Page, optionText: string) {
    const checkbox = page.locator('.el-checkbox').filter({ hasText: optionText }).first()
    await expect(checkbox).toBeVisible()
    if (!(await checkbox.evaluate((element) => element.classList.contains('is-checked')))) {
      await checkbox.click()
    }
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

  test('在线作答支持写作保存读回、提交待阅卷、人工阅卷保存和完成锁定', async ({ page }) => {
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
    await expect(page.getByText('答案已保存')).toBeVisible()
    page.once('dialog', (dialog) => {
      void dialog.accept()
    })
    await page.reload()
    await expect(page.getByText('答题卡')).toBeVisible()
    await expect(page.locator('.el-radio.is-checked').filter({ hasText: 'B. He goes to school every day.' })).toBeVisible()
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
    await page.getByRole('button', { name: '下一题' }).click()
    await expect(page.getByText('写作题').first()).toBeVisible()
    await expect(page.getByText('Write an essay about the importance of steady English practice.')).toBeVisible()
    const writingAnswer = 'Steady English practice matters because it turns small daily effort into lasting ability. I read, listen, and write every day.'
    const writingSaveResponse = page.waitForResponse((response) => {
      return response.url().includes('/api/exam/1/answers')
        && response.request().method() === 'POST'
        && Boolean(response.request().postData()?.includes('Steady English practice'))
    })
    await page.getByPlaceholder('请输入写作答案').fill(writingAnswer)
    await page.getByPlaceholder('请输入写作答案').blur()
    await expect((await writingSaveResponse).ok()).toBe(true)
    page.once('dialog', (dialog) => {
      void dialog.accept()
    })
    await page.reload()
    await expect(page.getByText('答题卡')).toBeVisible()
    await page.locator('.answer-card__item').filter({ hasText: /^5$/ }).click()
    await expect(page.getByPlaceholder('请输入写作答案')).toHaveValue(writingAnswer)
    await page.locator('.answer-card__item').filter({ hasText: /^2$/ }).click()
    await page.getByText('A. improve').click()
    await page.locator('.answer-card__item').filter({ hasText: /^3$/ }).click()
    await ensureCheckboxChecked(page, 'A. book')
    await ensureCheckboxChecked(page, 'C. teacher')
    await page.locator('.answer-card__item').filter({ hasText: /^4$/ }).click()
    await ensureCheckboxChecked(page, 'A. The learner practiced reading.')
    await ensureCheckboxChecked(page, 'B. The learner practiced listening.')
    await page.locator('.answer-card__item').filter({ hasText: /^5$/ }).click()
    await page.getByRole('article').getByRole('button', { name: '提交试卷' }).click()
    await page.getByRole('button', { name: '提交', exact: true }).click()

    await expect(page.getByText('本次考试已经提交，答案已锁定。')).toBeVisible()
    await expect(page.getByRole('article').filter({ hasText: '得分' }).getByRole('strong')).toHaveText('20')
    await expect(page.getByRole('article').filter({ hasText: '总分' }).getByRole('strong')).toHaveText('30')
    await expect(page.getByRole('article').filter({ hasText: '阅卷状态' }).getByRole('strong')).toHaveText('待阅卷')
    await expect(page.getByText(writingAnswer)).toBeVisible()
    await expect(page.getByText('正确答案：B')).toBeVisible()
    await expect(page.getByText('解析：Subject and verb agreement: He goes to school every day.')).toBeVisible()
    await page.getByRole('menuitem', { name: '我的成绩' }).click()
    await expect(page.getByRole('heading', { name: '我的成绩' })).toBeVisible()
    await page.getByRole('button', { name: '查看详情' }).first().click()
    await expect(page.getByText('正确答案：B')).toBeVisible()

    await page.locator('li.el-menu-item').filter({ hasText: /^考试管理$/ }).click()
    await expect(page.locator('.admin-page__header h1', { hasText: '考试管理' })).toBeVisible()
    await page.getByPlaceholder('搜索考试名称').fill('英语基础模拟考试')
    await page.getByRole('button', { name: '搜索' }).click()
    await englishExamRow(page).getByRole('button', { name: '成绩' }).click()
    const resultDrawer = page.getByRole('dialog', { name: /英语基础模拟考试 - 成绩/ })
    await expect(resultDrawer).toBeVisible()
    await expect(resultDrawer.getByText('待阅卷').first()).toBeVisible()
    await resultDrawer.getByRole('button', { name: '查看详情' }).first().click()
    await expect(page.getByRole('heading', { name: '英语基础模拟考试' })).toBeVisible()
    await expect(page.getByText(writingAnswer)).toBeVisible()
    await page.locator('.writing-review-form .el-input-number input').fill('8')
    await page.getByPlaceholder('阅卷评语').fill('观点清楚，结构完整')
    await page.getByRole('button', { name: '保存评分' }).click()
    await expect(page.getByText('评分已保存')).toBeVisible()
    await expect(page.getByText('观点清楚，结构完整')).toBeVisible()
    await page.reload()
    await expect(page.getByText('观点清楚，结构完整')).toBeVisible()
    await page.getByRole('button', { name: '完成阅卷' }).click()
    await expect(page.getByText('阅卷已完成')).toBeVisible()
    await expect(page.getByRole('article').filter({ hasText: '得分' }).getByRole('strong')).toHaveText('28')
    await expect(page.getByRole('article').filter({ hasText: '主观题分' }).getByRole('strong')).toHaveText('8')
    await expect(page.getByRole('article').filter({ hasText: '阅卷状态' }).getByRole('strong')).toHaveText('已出分')
    await expect(page.getByRole('button', { name: '保存评分' })).toHaveCount(0)

    expect(consoleIssues).toEqual([])
  })
})
