import { expect, test } from '@playwright/test'

import { collectConsoleIssues, downloadByButton, login, setNumberInput } from './helpers'

const CET4_EXAM = '2015年12月英语四级真题第一卷'
const CET4_AUDIO_PATH = 'public/local-assets/cet4/2015-12/set-1/201512cet4-01.mp3'
const CET4_AUDIO_URL = '/local-assets/cet4/2015-12/set-1/201512cet4-01.mp3'

test.describe('考试管理', () => {
  test('题库管理支持分类生命周期、搜索、新建和编辑取消', async ({ page }) => {
    const consoleIssues = collectConsoleIssues(page)
    await login(page)

    await page.getByRole('menuitem', { name: '题库管理' }).click()
    await expect(page.getByRole('heading', { name: '题库管理' })).toBeVisible()
    await expect(page.getByText('题库树')).toBeVisible()
    await expect.poll(async () => Math.round((await page.locator('.tree-pane .el-tree-node__content').first().boundingBox())?.height || 0)).toBeGreaterThanOrEqual(50)
    const categoryName = `浏览器分类${Date.now()}`
    const updatedCategoryName = `${categoryName}更新`
    await page.locator('.tree-pane').getByRole('button', { name: '新建分类' }).click()
    const categoryDialog = page.getByRole('dialog', { name: '新建分类' })
    await categoryDialog.getByLabel('名称').fill(categoryName)
    await categoryDialog.getByLabel('说明').fill('Playwright 创建分类')
    await categoryDialog.getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('分类已创建')).toBeVisible()
    await expect(page.locator('.tree-pane')).toContainText(categoryName)
    await page.locator('.tree-pane .bank-node--category').filter({ hasText: categoryName }).getByRole('button', { name: '编辑' }).click()
    const editCategoryDialog = page.getByRole('dialog', { name: '编辑分类' })
    await editCategoryDialog.getByLabel('名称').fill(updatedCategoryName)
    await editCategoryDialog.getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('分类已更新')).toBeVisible()
    const updatedCategoryRow = page.locator('.tree-pane .bank-node--category').filter({ hasText: updatedCategoryName })
    await expect(updatedCategoryRow).toBeVisible()
    await updatedCategoryRow.getByRole('button', { name: '新建题库' }).click()
    const categoryBankDialog = page.getByRole('dialog', { name: '新建题库' })
    const categoryBankName = `分类下题库${Date.now()}`
    await expect(categoryBankDialog.locator('.el-select').first()).toContainText(updatedCategoryName)
    await categoryBankDialog.getByLabel('名称').fill(categoryBankName)
    await categoryBankDialog.getByLabel('说明').fill('分类下创建题库')
    await categoryBankDialog.getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('题库已创建')).toBeVisible()
    await expect(page.locator('.tree-pane')).toContainText(categoryBankName)
    await updatedCategoryRow.getByRole('button', { name: '删除' }).click()
    await page.getByRole('dialog', { name: '删除分类' }).getByRole('button', { name: '删除分类' }).click()
    await expect(page.getByText('分类下存在题库，不能删除')).toBeVisible()
    const emptyCategoryName = `空分类${Date.now()}`
    await page.locator('.tree-pane').getByRole('button', { name: '新建分类' }).click()
    await page.getByRole('dialog', { name: '新建分类' }).getByLabel('名称').fill(emptyCategoryName)
    await page.getByRole('dialog', { name: '新建分类' }).getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('分类已创建')).toBeVisible()
    const emptyCategoryRow = page.locator('.tree-pane .bank-node--category').filter({ hasText: emptyCategoryName })
    await emptyCategoryRow.getByRole('button', { name: '删除' }).click()
    await page.getByRole('dialog', { name: '删除分类' }).getByRole('button', { name: '删除分类' }).click()
    await expect(page.getByText('分类已删除')).toBeVisible()
    await expect(page.locator('.tree-pane')).not.toContainText(emptyCategoryName)
    await page.getByPlaceholder('搜索题库或分类').fill('四级')
    await page.locator('.tree-pane').getByRole('button', { name: '搜索' }).click()
    await expect(page.locator('.tree-pane').getByText(CET4_EXAM)).toBeVisible()
    await page.locator('.admin-page__header').getByRole('button', { name: '新建题库' }).click()
    const bankDialog = page.getByRole('dialog', { name: '新建题库' })
    const bankName = `浏览器题库${Date.now()}`
    await bankDialog.getByLabel('名称').fill(bankName)
    await bankDialog.getByLabel('说明').fill('Playwright 创建题库')
    await bankDialog.getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('题库已创建')).toBeVisible()
    await expect(page.locator('.selected-bank')).toContainText(bankName)
    await page.getByPlaceholder('搜索题库或分类').fill(bankName)
    await page.locator('.tree-pane').getByRole('button', { name: '搜索' }).click()
    await expect(page.locator('.tree-pane')).toContainText(bankName)
    await page.locator('.selected-bank').getByRole('button', { name: '编辑题库' }).click()
    await expect(page.getByRole('dialog', { name: '编辑题库' })).toBeVisible()
    await page.getByRole('dialog', { name: '编辑题库' }).getByRole('button', { name: '取消' }).click()

    expect(consoleIssues.filter((issue) => !issue.includes('409 (Conflict)'))).toEqual([])
  })

  test('题库管理支持模板下载、导入、筛选、搜索、新建试题、上传和 URL 附件', async ({ page }) => {
    const consoleIssues = collectConsoleIssues(page)
    await login(page)

    await page.getByRole('menuitem', { name: '题库管理' }).click()
    await expect(page.getByRole('heading', { name: '题库管理' })).toBeVisible()
    const questionTemplate = await downloadByButton(page, '下载模板')
    await page.locator('.admin-page header input[type=file]').setInputFiles(questionTemplate)
    await expect(page.getByText('导入完成：成功')).toBeVisible()
    await page.locator('.tree-pane').getByText(CET4_EXAM).click()
    await expect(page.locator('.selected-bank')).toContainText(CET4_EXAM)
    await page.getByPlaceholder('搜索题干或题库').fill('Listening Comprehension')
    await page.locator('.question-pane').getByRole('button', { name: '搜索' }).click()
    await expect(page.getByText('Part II Listening Comprehension').first()).toBeVisible()
    await page.locator('.admin-page__header').getByRole('button', { name: '新建试题' }).click()
    const questionDialog = page.getByRole('dialog', { name: '新建试题' })
    await questionDialog.getByLabel('题干').fill(`浏览器创建试题 ${Date.now()}`)
    await questionDialog.locator('.option-row').nth(0).getByPlaceholder('选项内容').fill('正确选项')
    await questionDialog.locator('.option-row').nth(1).getByPlaceholder('选项内容').fill('错误选项')
    await questionDialog.getByRole('button', { name: '增加选项' }).click()
    await questionDialog.locator('.option-row').last().getByPlaceholder('选项内容').fill('干扰选项')
    await questionDialog.locator('input[type=file]').setInputFiles({
      name: 'browser-listening.mp3',
      mimeType: 'audio/mpeg',
      buffer: Buffer.from([1, 2, 3, 4]),
    })
    await expect(page.getByText('附件已上传')).toBeVisible()
    await expect(questionDialog.getByRole('link', { name: '打开附件' })).toBeVisible()
    await questionDialog.getByPlaceholder('输入图片、音频、视频或文件 URL').fill(CET4_AUDIO_URL)
    await questionDialog.locator('.url-attachment .el-select').click()
    await page.getByRole('option', { name: '音频' }).click()
    await questionDialog.getByRole('button', { name: '添加 URL' }).click()
    await expect(questionDialog.locator('.attachment-item')).toHaveCount(2)
    await questionDialog.locator('.attachment-item').last().getByRole('button', { name: '上移' }).click()
    await expect(questionDialog.locator('.attachment-item').first()).toContainText('201512cet4-01.mp3')
    await questionDialog.getByLabel('解析').fill('浏览器验收解析')
    await questionDialog.getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('试题已创建')).toBeVisible()
    await expect(page.locator('.selected-bank')).toContainText(CET4_EXAM)
    await expect(page.locator('.selected-bank')).toContainText(/[1-9]\d* 题/)

    expect(consoleIssues).toEqual([])
  })

  test('考试管理支持搜索、新建、保存草稿、发布和详情读回', async ({ page }) => {
    const consoleIssues = collectConsoleIssues(page)
    await login(page)

    await page.locator('li.el-menu-item').filter({ hasText: /^考试管理$/ }).click()
    await expect(page.locator('.admin-page__header h1', { hasText: '考试管理' })).toBeVisible()
    await page.getByPlaceholder('搜索考试名称').fill('四级')
    await page.getByRole('button', { name: '搜索' }).click()
    await expect(page.getByText(CET4_EXAM)).toBeVisible()
    await page.getByRole('row').filter({ hasText: CET4_EXAM }).getByRole('button', { name: '成绩' }).click()
    const resultDrawer = page.getByRole('dialog', { name: new RegExp(`${CET4_EXAM} - 成绩`) })
    await expect(resultDrawer).toBeVisible()
    await expect(resultDrawer).toContainText('考试人数')
    await expect(resultDrawer.locator('.result-summary strong').first()).toHaveText('0')
    await page.keyboard.press('Escape')
    await page.getByRole('button', { name: '新建考试' }).click()
    const examDialog = page.getByRole('dialog', { name: '新建考试' })
    await expect(examDialog.getByText('组卷信息')).toBeVisible()
    await expect(examDialog.getByText('试卷总分')).toBeVisible()
    await examDialog.locator('.rule-item .el-select').first().click()
    await page.getByRole('option', { name: CET4_EXAM }).click()
    await expect(examDialog.getByText(/单选题（可用 [1-9]/)).toBeVisible()
    await expect(examDialog.getByText(/写作题（可用 [1-9]/)).toBeVisible()
    const ruleNumbers = examDialog.locator('.rule-fields .el-input-number input')
    await setNumberInput(ruleNumbers.nth(0), '1')
    await setNumberInput(ruleNumbers.nth(1), '5')
    await setNumberInput(ruleNumbers.nth(2), '0')
    await setNumberInput(ruleNumbers.nth(3), '0')
    await setNumberInput(ruleNumbers.nth(4), '1')
    await setNumberInput(ruleNumbers.nth(5), '10')
    const examTitle = `浏览器考试 ${Date.now()}`
    await examDialog.getByLabel('考试名称').fill(examTitle)
    await examDialog.getByLabel('考试描述').fill('Playwright 创建考试')
    await examDialog.getByText('限定次数').click()
    await setNumberInput(examDialog.locator('.inline-control .el-input-number input'), '2')
    await examDialog.getByText('整卷一页').click()
    await examDialog.getByText('随机顺序').click()
    await examDialog.getByLabel('开放范围').getByText('公开考试').click()
    await examDialog.getByRole('button', { name: '按规则生成' }).click()
    await expect(examDialog.locator('.paper-table')).toContainText('Part II Listening Comprehension')
    await expect(examDialog.locator('.paper-table')).toContainText('Listening is more important than talking')
    await expect(examDialog.getByRole('button', { name: '更新试卷' })).toBeEnabled()
    await examDialog.locator('.manual-picker').getByRole('button', { name: '加载试题' }).click()
    await examDialog.locator('.picker-table').getByRole('button', { name: '加入试卷' }).first().click()
    await expect(examDialog.locator('.paper-table tbody tr')).toHaveCount(3)
    await examDialog.locator('.paper-table').getByRole('button', { name: '下移' }).first().click()
    await expect(examDialog.locator('.paper-table tbody tr').first()).not.toContainText('Part II Listening Comprehension')
    await examDialog.getByRole('button', { name: '预览试卷' }).click()
    await expect(page.getByRole('dialog', { name: '试卷预览' })).toBeVisible()
    await page.keyboard.press('Escape')
    await examDialog.getByRole('button', { name: '保存草稿' }).click()
    await expect(page.getByText('草稿已保存')).toBeVisible()
    const savedDialog = page.getByRole('dialog', { name: '编辑考试' })
    await expect(savedDialog.locator('.publish-summary strong').filter({ hasText: /^草稿$/ })).toBeVisible()
    await expect(savedDialog.locator('.paper-table tbody tr')).toHaveCount(3)
    await savedDialog.getByRole('button', { name: '发布考试' }).click()
    await expect(page.getByText('考试已发布')).toBeVisible()
    await expect(savedDialog.locator('.publish-summary strong').filter({ hasText: /^已发布$/ })).toBeVisible()
    await savedDialog.getByRole('button', { name: '取消' }).click()
    await page.getByPlaceholder('搜索考试名称').fill(examTitle)
    await page.getByRole('button', { name: '搜索' }).click()
    const savedRow = page.getByRole('row').filter({ hasText: examTitle, hasNotText: '副本' })
    const paperDownloadPromise = page.waitForEvent('download')
    await savedRow.getByRole('button', { name: '下载' }).click()
    await expect((await paperDownloadPromise).suggestedFilename()).toContain('.xlsx')
    await savedRow.getByRole('button', { name: '复制' }).click()
    await expect(page.getByText('试卷已复制')).toBeVisible()
    await page.getByRole('dialog', { name: '编辑考试' }).getByRole('button', { name: '取消' }).click()
    await savedRow.getByRole('button', { name: '撤销发布' }).click()
    await page.getByRole('dialog', { name: '撤销发布' }).getByRole('button', { name: '撤销发布' }).click()
    await expect(page.getByText('考试已撤销发布')).toBeVisible()
    await page.getByRole('row').filter({ hasText: examTitle, hasNotText: '副本' }).getByRole('button', { name: '编辑' }).click()
    const editDialog = page.getByRole('dialog', { name: '编辑考试' })
    await expect(editDialog).toBeVisible()
    await expect(editDialog.locator('.rule-item .el-select')).toContainText(CET4_EXAM)
    await expect(editDialog.locator('.paper-table tbody tr')).toHaveCount(3)
    await expect(editDialog.locator('.publish-summary strong').filter({ hasText: /^草稿$/ })).toBeVisible()
    await editDialog.getByRole('button', { name: '取消' }).click()

    expect(consoleIssues).toEqual([])
  })

  test('考试草稿不会被题库编辑魔法更新，显式更新试卷后才持久化新快照', async ({ page }) => {
    const consoleIssues = collectConsoleIssues(page)
    await login(page)
    const token = await page.evaluate(() => window.localStorage.getItem('kaoshi.accessToken'))
    const headers = { Authorization: `Bearer ${token}` }
    const suffix = Date.now()
    const originalStem = `草稿快照原题干 ${suffix}`
    const changedStem = `草稿快照新题干 ${suffix}`
    const examTitle = `无魔法草稿考试 ${suffix}`

    const bankResponse = await page.request.post('/api/admin/question-banks', {
      headers,
      data: {
        categoryId: 1,
        name: `无魔法题库 ${suffix}`,
        description: 'Playwright 快照夹具',
        status: 'ACTIVE',
      },
    })
    expect(bankResponse.ok()).toBeTruthy()
    const bank = (await bankResponse.json()).data

    const questionResponse = await page.request.post('/api/admin/questions', {
      headers,
      data: {
        bankId: bank.id,
        type: 'SINGLE_CHOICE',
        stem: originalStem,
        analysis: '原解析',
        difficulty: 'EASY',
        status: 'ACTIVE',
        options: [
          { label: 'A', content: '错误', correct: false },
          { label: 'B', content: '正确', correct: true },
        ],
        attachments: [],
      },
    })
    expect(questionResponse.ok()).toBeTruthy()
    const question = (await questionResponse.json()).data

    const examResponse = await page.request.post('/api/admin/exams', {
      headers,
      data: {
        title: examTitle,
        description: '验证题库编辑不会隐式改写草稿',
        qualifyScore: 0,
        startTime: '2026-01-01T00:00:00',
        endTime: '2026-12-31T23:59:59',
        durationMinutes: 20,
        timeLimit: false,
        attemptLimit: null,
        displayMode: 'ALL',
        questionOrderMode: 'FIXED',
        openType: 'PUBLIC',
        departmentIds: [],
        rules: [
          {
            bankId: bank.id,
            singleCount: 1,
            singleScore: 5,
            multipleCount: 0,
            multipleScore: 0,
            writingCount: 0,
            writingScore: 0,
          },
        ],
      },
    })
    expect(examResponse.ok()).toBeTruthy()

    await page.locator('li.el-menu-item').filter({ hasText: /^考试管理$/ }).click()
    await page.getByPlaceholder('搜索考试名称').fill(examTitle)
    await page.getByRole('button', { name: '搜索' }).click()
    await page.getByRole('row').filter({ hasText: examTitle }).getByRole('button', { name: '编辑' }).click()
    let examDialog = page.getByRole('dialog', { name: '编辑考试' })
    await expect(examDialog.locator('.paper-table')).toContainText(originalStem)
    await examDialog.getByRole('button', { name: '取消' }).click()

    const updateQuestionResponse = await page.request.put(`/api/admin/questions/${question.id}`, {
      headers,
      data: {
        bankId: bank.id,
        type: 'SINGLE_CHOICE',
        stem: changedStem,
        analysis: '新解析',
        difficulty: 'EASY',
        status: 'ACTIVE',
        options: [
          { label: 'A', content: '正确', correct: true },
          { label: 'B', content: '错误', correct: false },
        ],
        attachments: [],
      },
    })
    expect(updateQuestionResponse.ok()).toBeTruthy()

    await page.getByRole('row').filter({ hasText: examTitle }).getByRole('button', { name: '编辑' }).click()
    examDialog = page.getByRole('dialog', { name: '编辑考试' })
    await expect(examDialog.locator('.paper-table')).toContainText(originalStem)
    await expect(examDialog.locator('.paper-table')).not.toContainText(changedStem)
    await examDialog.getByRole('button', { name: '更新试卷' }).click()
    await expect(examDialog.locator('.paper-table')).toContainText(changedStem)
    await examDialog.getByRole('button', { name: '保存草稿' }).click()
    await expect(page.getByText('草稿已保存')).toBeVisible()
    await page.getByRole('dialog', { name: '编辑考试' }).getByRole('button', { name: '取消' }).click()

    await page.getByRole('row').filter({ hasText: examTitle }).getByRole('button', { name: '编辑' }).click()
    examDialog = page.getByRole('dialog', { name: '编辑考试' })
    await expect(examDialog.locator('.paper-table')).toContainText(changedStem)
    await expect(examDialog.locator('.paper-table')).not.toContainText(originalStem)
    await examDialog.getByRole('button', { name: '取消' }).click()

    expect(consoleIssues).toEqual([])
  })
})
