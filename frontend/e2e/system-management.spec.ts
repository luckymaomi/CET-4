import { expect, test } from '@playwright/test'

import { chooseSelectOption, collectConsoleIssues, login } from './helpers'

test.describe('系统管理', () => {
  test('导航展示当前系统与考试入口', async ({ page }) => {
    const consoleIssues = collectConsoleIssues(page)
    await login(page)

    await expect(page.getByRole('menuitem', { name: '控制台' })).toBeVisible()
    await expect(page.locator('.el-sub-menu__title').filter({ hasText: '在线考试' })).toBeVisible()
    await expect(page.locator('.el-sub-menu__title').filter({ hasText: '考试管理' })).toBeVisible()
    await expect(page.locator('.el-sub-menu__title').filter({ hasText: '系统管理' })).toBeVisible()
    await expect(page.getByRole('menuitem', { name: '用户管理' })).toBeVisible()
    await expect(page.getByRole('menuitem', { name: '角色管理' })).toBeVisible()
    await expect(page.getByRole('menuitem', { name: '部门管理' })).toBeVisible()

    expect(consoleIssues).toEqual([])
  })

  test('角色管理支持新建、全选、清空和编辑取消', async ({ page }) => {
    const consoleIssues = collectConsoleIssues(page)
    await login(page)

    await page.getByRole('menuitem', { name: '角色管理' }).click()
    await expect(page.getByRole('heading', { name: '角色管理' })).toBeVisible()
    await page.getByRole('button', { name: '新建角色' }).click()
    const roleDialog = page.getByRole('dialog', { name: '新建角色' })
    await expect(roleDialog).toBeVisible()
    await page.getByRole('button', { name: '清空' }).first().click()
    await page.getByRole('button', { name: '清空' }).nth(1).click()
    await page.getByRole('button', { name: '全选' }).first().click()
    await page.getByRole('button', { name: '全选' }).nth(1).click()
    await page.getByLabel('角色名称').fill('浏览器验收员')
    await page.getByLabel('角色编码').fill(`E2E_AUDITOR_${Date.now()}`)
    await page.getByLabel('说明').fill('Playwright 真实端到端测试创建')
    await roleDialog.getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('角色已创建')).toBeVisible()
    await expect(page.getByText('浏览器验收员')).toBeVisible()
    await page.getByRole('row').filter({ hasText: '浏览器验收员' }).getByRole('button', { name: '编辑' }).click()
    await expect(page.getByRole('dialog', { name: '编辑角色' })).toBeVisible()
    await page.getByRole('dialog', { name: '编辑角色' }).getByRole('button', { name: '取消' }).click()

    expect(consoleIssues).toEqual([])
  })

  test('用户管理支持搜索、新建、编辑和禁用', async ({ page }) => {
    const consoleIssues = collectConsoleIssues(page)
    await login(page)

    await page.getByRole('menuitem', { name: '用户管理' }).click()
    await expect(page.getByRole('heading', { name: '用户管理' })).toBeVisible()
    await page.getByPlaceholder('搜索账号或姓名').fill('admin')
    await page.getByRole('button', { name: '搜索' }).click()
    await expect(page.getByRole('cell', { name: 'admin' })).toBeVisible()
    await page.getByPlaceholder('搜索账号或姓名').fill('')
    await page.getByRole('button', { name: '搜索' }).click()
    await page.getByRole('button', { name: '新建用户' }).click()
    const userDialog = page.getByRole('dialog', { name: '新建用户' })
    const username = `e2e_user_${Date.now()}`
    await userDialog.getByLabel('账号').fill(username)
    await userDialog.getByLabel('姓名').fill('浏览器用户')
    await expect(userDialog.getByLabel('密码')).toHaveCount(0)
    await chooseSelectOption(page, userDialog, '角色', '考生 (STUDENT)')
    await userDialog.getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('用户已创建')).toBeVisible()
    const userRow = page.getByRole('row').filter({ hasText: username })
    await expect(userRow).toBeVisible()
    await userRow.getByRole('button', { name: '编辑' }).click()
    const editUserDialog = page.getByRole('dialog', { name: '编辑用户' })
    await editUserDialog.getByLabel('姓名').fill('浏览器用户已编辑')
    await editUserDialog.getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('用户已更新')).toBeVisible()
    await page.getByRole('row').filter({ hasText: username }).getByRole('button', { name: '禁用' }).click()
    await expect(page.getByText('用户状态已更新')).toBeVisible()

    expect(consoleIssues).toEqual([])
  })

  test('部门管理支持在线新建、编码生成、编辑和删除', async ({ page }) => {
    const consoleIssues = collectConsoleIssues(page)
    await login(page)

    await page.getByRole('menuitem', { name: '部门管理' }).click()
    await expect(page.getByRole('heading', { name: '部门管理' })).toBeVisible()
    await page.getByRole('button', { name: '新建部门' }).click()
    const departmentDialog = page.getByRole('dialog', { name: '新建部门' })
    const departmentCodeInput = departmentDialog.locator('.el-form-item').filter({ hasText: '部门编码' }).locator('input')
    await departmentDialog.getByLabel('部门名称').fill('浏览器验收部')
    await expect(departmentCodeInput).toBeDisabled()
    await expect(departmentCodeInput).not.toHaveValue('')
    await expect(departmentDialog.getByLabel('排序')).toHaveCount(0)
    await departmentDialog.getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('部门已创建')).toBeVisible()
    const departmentRow = page.getByRole('row').filter({ hasText: '浏览器验收部' })
    await departmentRow.getByRole('button', { name: '编辑' }).click()
    await expect(page.getByRole('dialog', { name: '编辑部门' })).toBeVisible()
    await page.getByRole('dialog', { name: '编辑部门' }).getByRole('button', { name: '取消' }).click()
    await departmentRow.getByRole('button', { name: '删除' }).click()
    await page.getByRole('dialog', { name: '删除部门' }).getByRole('button', { name: '删除' }).click()
    await expect(page.getByText('部门已删除')).toBeVisible()

    expect(consoleIssues).toEqual([])
  })
})
