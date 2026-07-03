import { expect, test, type Page } from '@playwright/test'

const username = process.env.E2E_ADMIN_USERNAME ?? 'e2e-admin'
const password = process.env.E2E_ADMIN_PASSWORD ?? 'E2e-Only-Password-2026!'
const adminBaseURL = (process.env.E2E_ADMIN_BASE_URL ?? 'http://admin.localhost')
  .replace(/\/$/, '')

const signIn = async (page: Page) => {
  await page.goto(`${adminBaseURL}/login`)
  await page.getByLabel('用户名').fill(username)
  await page.getByLabel('密码').fill(password)
  await page.getByRole('button', { name: '进入控制室' }).click()
  await expect(page).toHaveURL(`${adminBaseURL}/`)
}

test.describe('admin publishing journey', () => {
  test('logs in, creates, publishes and exposes an article', async ({ page }) => {
    const slug = `e2e-${Date.now()}`
    const title = `E2E 发布验证 ${slug}`

    await signIn(page)
    await page.goto(`${adminBaseURL}/articles/new`)
    await page.getByPlaceholder('文章标题').fill(title)
    await page.getByPlaceholder('my-first-article').fill(slug)
    await page.getByPlaceholder('# 从这里开始写…').fill('# 自动化验收\n\n这篇文章由端到端测试创建。')
    await page.getByRole('button', { name: '创建草稿' }).click()
    await expect(page).toHaveURL(/\/articles\/\d+$/)

    await page.getByRole('link', { name: /返回文章列表/ }).click()
    const row = page.locator('.article-row').filter({ hasText: title })
    await expect(row).toBeVisible()
    await row.getByRole('button', { name: '发布' }).click()
    await expect(row).toContainText('已发布')

    await page.goto(`/article/${slug}`)
    await expect(page.getByRole('heading', { name: title })).toBeVisible()
    await expect(page.locator('.prose')).toContainText('这篇文章由端到端测试创建')
  })

  test('rejects an invalid login without leaking credentials', async ({ page }) => {
    await page.goto(`${adminBaseURL}/login`)
    await page.getByLabel('用户名').fill('unknown-user')
    await page.getByLabel('密码').fill('wrong-password-value')
    await page.getByRole('button', { name: '进入控制室' }).click()

    await expect(page.getByRole('alert')).toBeVisible()
    await expect(page.getByRole('alert')).not.toContainText('wrong-password-value')
  })
})
