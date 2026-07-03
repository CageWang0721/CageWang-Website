import { expect, test } from '@playwright/test'

test.describe('public blog', () => {
  test('@smoke renders the home page and primary navigation', async ({ page }) => {
    await page.goto('/')

    await expect(page).toHaveTitle(/CageWang‘s Blog/)
    await expect(page.getByRole('heading', { name: /留一页余白/ })).toBeVisible()
    await expect(page.getByRole('link', { name: '文章', exact: true }).first()).toBeVisible()
    await expect(page.getByRole('link', { name: '留言', exact: true }).first()).toBeVisible()
  })

  test('@smoke persists dark mode across navigation', async ({ page }) => {
    await page.goto('/')

    await page.getByRole('button', { name: '切换到深色模式' }).click()
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
    await page.getByRole('link', { name: '文章', exact: true }).first().click()
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  })

  test('@smoke exposes machine-readable discovery endpoints', async ({ request }) => {
    const [health, rss, sitemap, robots] = await Promise.all([
      request.get('/api/v1/status'),
      request.get('/rss.xml'),
      request.get('/sitemap.xml'),
      request.get('/robots.txt')
    ])

    expect(health.ok()).toBeTruthy()
    expect((await health.json()).status).toBe('ok')
    expect(rss.ok()).toBeTruthy()
    expect(await rss.text()).toContain('<rss')
    expect(sitemap.ok()).toBeTruthy()
    expect(await sitemap.text()).toContain('<urlset')
    expect(robots.ok()).toBeTruthy()
  })
})
