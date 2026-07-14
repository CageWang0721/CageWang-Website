import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const pagePath = new URL('../app/pages/index.vue', import.meta.url)
const cssPath = new URL('../app/assets/css/main.css', import.meta.url)
const packagePath = new URL('../package.json', import.meta.url)

const [pageSource, cssSource, packageSource] = await Promise.all([
  readFile(pagePath, 'utf8'),
  readFile(cssPath, 'utf8'),
  readFile(packagePath, 'utf8')
])

test('profile card exposes the four requested social platforms', () => {
  for (const platform of ['GitHub', '抖音', 'B站', '小红书']) {
    assert.match(pageSource, new RegExp(`label:\\s*['\"]${platform}['\"]`))
  }
})

test('social links replace the Archive and RSS profile shortcuts', () => {
  assert.doesNotMatch(pageSource, /to="\/archive" aria-label="文章归档"/)
  assert.doesNotMatch(pageSource, /href="\/rss\.xml" aria-label="RSS"/)
})

test('placeholder social URLs are centralized and cannot jump the page', () => {
  const runtimeFallbacks = pageSource.match(
    /config\.public\.social(?:Github|Douyin|Bilibili|Xiaohongshu)Url\s*\|\|\s*['"]#['"]/g
  ) || []
  assert.equal(runtimeFallbacks.length, 4)
  assert.match(pageSource, /preventDefault\(\)/)
})

test('profile links use maintained icons and the approved four-column layout', () => {
  const packageJson = JSON.parse(packageSource)
  assert.ok(packageJson.dependencies?.['simple-icons'])
  assert.match(pageSource, /from ['"]simple-icons['"]/)
  assert.match(cssSource, /\.profile-links\s*\{[^}]*grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\)/s)
})
