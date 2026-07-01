<script setup lang="ts">
const route = useRoute()
const theme = useTheme()

const links = [
  { label: '首页', to: '/' },
  { label: '文章', to: '/blog' },
  { label: '分类', to: '/category' },
  { label: '标签', to: '/tag' },
  { label: '归档', to: '/archive' }
]

const isActive = (to: string) =>
  to === '/' ? route.path === '/' : route.path.startsWith(to)
</script>

<template>
  <div class="site-shell">
    <header class="site-header">
      <div class="nav-wrap">
        <NuxtLink class="brand" to="/" aria-label="回到首页">
          <span class="brand-mark">余</span>
          <span>余白札记</span>
        </NuxtLink>

        <nav class="desktop-nav" aria-label="主导航">
          <NuxtLink
            v-for="link in links"
            :key="link.to"
            :to="link.to"
            :class="{ active: isActive(link.to) }"
          >
            {{ link.label }}
          </NuxtLink>
        </nav>

        <div class="nav-actions">
          <NuxtLink class="icon-button" to="/search" aria-label="搜索文章">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="11" cy="11" r="6.5" />
              <path d="m16 16 4 4" />
            </svg>
          </NuxtLink>
          <button
            class="icon-button"
            type="button"
            :aria-label="theme.isDark.value ? '切换到浅色模式' : '切换到深色模式'"
            @click="theme.toggle"
          >
            <svg v-if="theme.isDark.value" viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="12" cy="12" r="4" />
              <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
            </svg>
            <svg v-else viewBox="0 0 24 24" aria-hidden="true">
              <path d="M20.5 15.5A8.5 8.5 0 0 1 8.5 3.5a8.5 8.5 0 1 0 12 12Z" />
            </svg>
          </button>
        </div>
      </div>

      <nav class="mobile-nav" aria-label="移动端导航">
        <NuxtLink
          v-for="link in links"
          :key="link.to"
          :to="link.to"
          :class="{ active: isActive(link.to) }"
        >
          {{ link.label }}
        </NuxtLink>
      </nav>
    </header>

    <main>
      <NuxtPage />
    </main>

    <footer class="site-footer">
      <div>
        <p class="footer-title">余白札记</p>
        <p>在代码、生活和那些尚未命名的念头之间，留一点呼吸。</p>
      </div>
      <div class="footer-links">
        <NuxtLink to="/archive">归档</NuxtLink>
        <a href="/rss.xml">RSS</a>
        <a href="/sitemap.xml">Sitemap</a>
      </div>
      <p class="copyright">© {{ new Date().getFullYear() }} · Built with care and Nuxt.</p>
    </footer>
  </div>
</template>
