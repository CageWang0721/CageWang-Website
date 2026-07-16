<script setup lang="ts">
import weatherMoonIcon from '@fluentui/svg-icons/icons/weather_moon_24_regular.svg?url'
import weatherSunnyIcon from '@fluentui/svg-icons/icons/weather_sunny_24_regular.svg?url'

const route = useRoute()
const theme = useTheme()
const isScrolled = ref(false)

const navigationLinks = [
  { label: '首页', to: '/', icon: 'icon-zhuye' },
  { label: '文章', to: '/blog', icon: 'icon-boke' },
  { label: '分类', to: '/category', icon: 'icon-folder' },
  { label: '标签', to: '/tag', icon: 'icon-biaoqian' },
  { label: '归档', to: '/archive', icon: 'icon-guidang' }
]

const isActiveRoute = (path: string) =>
  path === '/' ? route.path === '/' : route.path.startsWith(path)

const updateScrollState = () => {
  isScrolled.value = window.scrollY > 48
}

onMounted(() => {
  updateScrollState()
  window.addEventListener('scroll', updateScrollState, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', updateScrollState)
})
</script>

<template>
  <header class="site-header" :class="{ scrolled: isScrolled }">
    <div class="nav-wrap">
      <NuxtLink class="brand" to="/" aria-label="回到首页">
        <span>Wineclouds’Blog</span>
      </NuxtLink>

      <nav class="desktop-nav" aria-label="主导航">
        <NuxtLink
          v-for="link in navigationLinks"
          :key="link.to"
          :to="link.to"
          :class="{ active: isActiveRoute(link.to) }"
        >
          <i class="iconfont" :class="link.icon" aria-hidden="true" />
          {{ link.label }}
        </NuxtLink>
      </nav>

      <div class="nav-actions">
        <NuxtLink class="icon-button" to="/search" aria-label="搜索文章">
          <i class="iconfont icon-sousuo" aria-hidden="true" />
        </NuxtLink>

        <button
          class="theme-button"
          type="button"
          :aria-label="theme.isDark.value ? '切换到浅色模式' : '切换到深色模式'"
          :title="theme.isDark.value ? '浅色模式' : '深色模式'"
          @click="theme.toggle"
        >
          <img
            :src="theme.isDark.value ? weatherSunnyIcon : weatherMoonIcon"
            alt=""
          >
        </button>
      </div>
    </div>

    <nav class="mobile-nav" aria-label="移动端导航">
      <NuxtLink
        v-for="link in navigationLinks"
        :key="link.to"
        :to="link.to"
        :class="{ active: isActiveRoute(link.to) }"
      >
        <i class="iconfont" :class="link.icon" aria-hidden="true" />
        {{ link.label }}
      </NuxtLink>
    </nav>
  </header>
</template>
