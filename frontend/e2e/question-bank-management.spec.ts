import { expect, test, type Page } from '@playwright/test'

import { collectConsoleIssues, downloadByButton, login } from './helpers'

const CET4_BANK = '四级样例题库'
const CET4_AUDIO_URL = '/local-assets/cet4/2023-03/set-1/2023-03-cet4-listening.mp3'

async function chooseCategoryAction(page: Page, categoryName: string, actionName: '新建题库' | '编辑分类' | '删除分类') {
  const categoryRow = page.locator('.tree-pane .bank-node--category').filter({ hasText: categoryName })
  await categoryRow.hover()
  await categoryRow.getByRole('button', { name: '操作' }).click()
  await page.getByRole('menuitem', { name: actionName }).click()
}

test.describe('题库管理', () => {
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
    await chooseCategoryAction(page, categoryName, '编辑分类')
    const editCategoryDialog = page.getByRole('dialog', { name: '编辑分类' })
    await editCategoryDialog.getByLabel('名称').fill(updatedCategoryName)
    await editCategoryDialog.getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('分类已更新')).toBeVisible()
    const updatedCategoryRow = page.locator('.tree-pane .bank-node--category').filter({ hasText: updatedCategoryName })
    await expect(updatedCategoryRow).toBeVisible()
    await chooseCategoryAction(page, updatedCategoryName, '新建题库')
    const categoryBankDialog = page.getByRole('dialog', { name: '新建题库' })
    const categoryBankName = `分类下题库${Date.now()}`
    await expect(categoryBankDialog.locator('.el-select').first()).toContainText(updatedCategoryName)
    await categoryBankDialog.getByLabel('名称').fill(categoryBankName)
    await categoryBankDialog.getByLabel('说明').fill('分类下创建题库')
    await categoryBankDialog.getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('题库已创建')).toBeVisible()
    await expect(page.locator('.tree-pane')).toContainText(categoryBankName)
    await chooseCategoryAction(page, updatedCategoryName, '删除分类')
    await page.getByRole('dialog', { name: '删除分类' }).getByRole('button', { name: '删除分类' }).click()
    await expect(page.getByText('分类下存在题库，不能删除')).toBeVisible()
    const emptyCategoryName = `空分类${Date.now()}`
    await page.locator('.tree-pane').getByRole('button', { name: '新建分类' }).click()
    await page.getByRole('dialog', { name: '新建分类' }).getByLabel('名称').fill(emptyCategoryName)
    await page.getByRole('dialog', { name: '新建分类' }).getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('分类已创建')).toBeVisible()
    const emptyCategoryRow = page.locator('.tree-pane .bank-node--category').filter({ hasText: emptyCategoryName })
    await chooseCategoryAction(page, emptyCategoryName, '删除分类')
    await page.getByRole('dialog', { name: '删除分类' }).getByRole('button', { name: '删除分类' }).click()
    await expect(page.getByText('分类已删除')).toBeVisible()
    await expect(page.locator('.tree-pane')).not.toContainText(emptyCategoryName)
    await page.getByPlaceholder('搜索题库或分类').fill('四级')
    await page.locator('.tree-pane').getByRole('button', { name: '搜索' }).click()
    await expect(page.locator('.tree-pane').getByText(CET4_BANK)).toBeVisible()
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
    await page.locator('.tree-pane').getByText(CET4_BANK).click()
    await expect(page.locator('.selected-bank')).toContainText(CET4_BANK)
    await expect(page.locator('.selected-bank').getByRole('button', { name: '题组结构' })).toHaveCount(0)
    await page.getByPlaceholder('搜索题干或题库').fill('Excel import listening question')
    await page.locator('.question-pane').getByRole('button', { name: '搜索' }).click()
    await expect(page.getByText('Excel import listening question').first()).toBeVisible()
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
    await expect(questionDialog.locator('.attachment-item').first()).toContainText('2023-03-cet4-listening.mp3')
    await questionDialog.getByLabel('解析').fill('浏览器验收解析')
    await questionDialog.getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('试题已创建')).toBeVisible()
    await expect(page.locator('.selected-bank')).toContainText(CET4_BANK)
    await expect(page.locator('.selected-bank')).toContainText(/[1-9]\d* 题/)

    expect(consoleIssues).toEqual([])
  })
})
