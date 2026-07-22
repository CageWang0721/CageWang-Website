<script setup lang="ts">
import {
  siBilibili,
  siGithub,
  siTiktok,
  siXiaohongshu,
  type SimpleIcon
} from 'simple-icons'

const api = useBlogApi()
const { data, error, refresh } = await useAsyncData('home', () => api.home())
const { data: profile, refresh: refreshProfile } = await useAsyncData('site-profile', () => api.profile())
const { data: githubContributions, refresh: refreshGitHubContributions } = await useAsyncData(
  'github-contributions',
  () => api.githubContributions()
)
const config = useRuntimeConfig()
const avatarSrc = computed(() => profile.value?.avatarUrl || '/images/wineclouds-avatar.webp')
const signature = computed(() => profile.value?.signature || '本质哈基米')
const siteStats = useSiteStatistics()
const siteTimeZone = 'Asia/Shanghai'
const motionReady = ref(false)

const getSiteDateParts = (date: Date) => {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: siteTimeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    weekday: 'long'
  }).formatToParts(date)
  const values = Object.fromEntries(parts.map(part => [part.type, part.value]))

  return {
    year: Number(values.year),
    month: Number(values.month),
    day: Number(values.day),
    weekday: values.weekday || ''
  }
}

const currentTime = ref(new Date())
const clockLabel = computed(() => new Intl.DateTimeFormat('zh-CN', {
  timeZone: siteTimeZone,
  hour: '2-digit',
  minute: '2-digit',
  hour12: false
}).format(currentTime.value))
const clockDateLabel = computed(() => {
  const current = getSiteDateParts(currentTime.value)
  return `${current.weekday.toUpperCase()} ${String(current.day).padStart(2, '0')}/${String(current.month).padStart(2, '0')}`
})
const calendarDate = computed(() => getSiteDateParts(currentTime.value))
const calendarLabel = computed(() => `${calendarDate.value.year}年${calendarDate.value.month}月`)
const calendarCells = computed<Array<number | null>>(() => {
  const leadingEmptyCells = new Date(Date.UTC(calendarDate.value.year, calendarDate.value.month - 1, 1)).getUTCDay()
  const daysInMonth = new Date(Date.UTC(calendarDate.value.year, calendarDate.value.month, 0)).getUTCDate()
  return [
    ...Array.from({ length: leadingEmptyCells }, () => null),
    ...Array.from({ length: daysInMonth }, (_, index) => index + 1)
  ]
})
const calendarToday = computed(() => calendarDate.value.day)
let clockInterval: number | undefined
let motionFrame: number | undefined
let articleObserver: IntersectionObserver | undefined

const socialLinks: Array<{ label: string, href: string, icon: SimpleIcon }> = [
  { label: 'GitHub', href: config.public.socialGithubUrl || 'https://github.com/Wineclouds04', icon: siGithub },
  {
    label: '抖音',
    href: config.public.socialDouyinUrl || 'https://www.douyin.com/user/MS4wLjABAAAAyDLG3rv8oGhFrBfz-5KxjozQW0sRV1rlg1oIIWdAkjxuXYDD7ueG1q1J-F02dwoz?from_tab_name=main',
    icon: siTiktok
  },
  { label: 'B站', href: config.public.socialBilibiliUrl || 'https://space.bilibili.com/406310101', icon: siBilibili },
  {
    label: '小红书',
    href: config.public.socialXiaohongshuUrl || 'https://www.xiaohongshu.com/user/profile/64c5ebe9000000000b00612b?xsec_token=ABVLi2q8QwizAK7oUjjyHM0ou6_fklAErwxEqBXJ8Jj2s%3D&xsec_source=pc_search',
    icon: siXiaohongshu
  }
]

const shownArticles = computed(() => data.value?.latest.slice(0, 8) || [])
const contributionCells = computed(() => {
  const days = githubContributions.value?.days.slice(-70) || []
  if (!days.length) {
    return Array.from({ length: 70 }, (_, index) => ({
      date: `empty-${index}`,
      contributionCount: 0,
      level: 0
    }))
  }
  const maximum = Math.max(0, ...days.map(day => day.contributionCount))

  return days.map(day => ({
    ...day,
    level: day.contributionCount === 0 || maximum === 0
      ? 0
      : Math.min(4, Math.ceil((day.contributionCount / maximum) * 4))
  }))
})
const refreshHome = () => {
  void refresh()
  void refreshProfile()
  void refreshGitHubContributions()
}

const refreshWhenVisible = () => {
  if (document.visibilityState === 'visible') refreshHome()
}

const setupArticleReveal = () => {
  articleObserver?.disconnect()
  const articleCards = Array.from(
    document.querySelectorAll<HTMLElement>('.home-article-column > .article-card')
  )

  if (!('IntersectionObserver' in window)) {
    articleCards.forEach(card => card.classList.add('motion-in-view'))
    return
  }

  articleObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return
      entry.target.classList.add('motion-in-view')
      articleObserver?.unobserve(entry.target)
    })
  }, {
    rootMargin: '0px 0px -8% 0px',
    threshold: 0.12
  })

  articleCards.forEach((card) => {
    const bounds = card.getBoundingClientRect()
    if (bounds.top < window.innerHeight * 0.92 && bounds.bottom > 0) {
      card.classList.add('motion-in-view')
      return
    }
    articleObserver?.observe(card)
  })
}

watch(shownArticles, () => {
  void nextTick(setupArticleReveal)
}, { flush: 'post' })

onMounted(() => {
  window.addEventListener('focus', refreshHome)
  document.addEventListener('visibilitychange', refreshWhenVisible)
  void nextTick(setupArticleReveal)
  motionFrame = window.requestAnimationFrame(() => {
    motionReady.value = true
  })
  clockInterval = window.setInterval(() => {
    currentTime.value = new Date()
  }, 1000)
})

onBeforeUnmount(() => {
  window.removeEventListener('focus', refreshHome)
  document.removeEventListener('visibilitychange', refreshWhenVisible)
  articleObserver?.disconnect()
  if (motionFrame !== undefined) window.cancelAnimationFrame(motionFrame)
  if (clockInterval !== undefined) window.clearInterval(clockInterval)
})

useSeoMeta({
  title: 'Wineclouds’Blog · 记录技术、生活与灵光',
  description: '在代码、生活和那些尚未命名的念头之间，留一点呼吸。',
  ogTitle: 'Wineclouds’Blog',
  ogDescription: '记录技术、生活与灵光的个人博客。',
  ogType: 'website',
  twitterCard: 'summary_large_image'
})
</script>

<template>
  <div class="reference-home" :class="{ 'motion-ready': motionReady }">
    <section class="home-feed-section">
      <div class="home-feed-layout">
        <aside class="home-sidebar-left" aria-label="博主与分类信息">
          <section class="profile-card profile-card--reference">
            <div class="profile-avatar-frame">
              <img class="profile-avatar" :src="avatarSrc" alt="Wineclouds 的头像">
            </div>
            <h2>Wineclouds</h2>
            <p>{{ signature }}</p>

            <div class="profile-links" aria-label="社交链接">
              <a
                v-for="link in socialLinks"
                :key="link.label"
                :href="link.href"
                :aria-label="link.label"
                :title="link.label"
                target="_blank"
                rel="noopener noreferrer"
              >
                <svg class="profile-social-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                  <path :d="link.icon.path" fill="currentColor" />
                </svg>
              </a>
            </div>
          </section>

          <section class="home-panel notice-card">
            <h2>公告</h2>
            <p>欢迎来到我的博客！记录技术、创作与生活里的闪光瞬间。</p>
            <NuxtLink class="panel-link" to="/blog">开始阅读 <span aria-hidden="true">→</span></NuxtLink>
          </section>

          <section class="home-panel contribution-card" aria-label="GitHub 贡献图">
            <div class="panel-heading-row">
              <h2>GitHub</h2>
            </div>
            <div class="contribution-grid" aria-label="最近 70 天 GitHub 贡献">
              <span
                v-for="(day, index) in contributionCells"
                :key="day.date"
                :class="`level-${day.level}`"
                :style="{ '--motion-order': index }"
                :title="`${day.date}: ${day.contributionCount} 次贡献`"
              />
            </div>
          </section>

        </aside>

        <div class="home-article-column">
          <template v-if="shownArticles.length">
            <ArticleCard
              v-for="(article, index) in shownArticles"
              :key="article.id"
              :article="article"
              :index="index"
              :style="{ '--motion-order': index }"
            />
          </template>

          <EmptyState
            v-else
            :description="error ? '暂时无法连接内容服务，请稍后再试。' : undefined"
          />
        </div>

        <aside class="home-sidebar-right" aria-label="站点信息与日历">
          <section class="home-panel greeting-card">
            <div class="greeting-copy">
              <p>你好，愿你今天也有好心情！</p>
              <strong>{{ clockLabel }}</strong>
              <small>{{ clockDateLabel }}</small>
            </div>
            <div class="greeting-scene" aria-hidden="true" />
          </section>

          <section class="home-panel site-info-card">
            <h2>站点信息</h2>
            <dl>
              <div><dt>在线访客</dt><dd>{{ siteStats.onlineVisitors.toLocaleString() }}</dd></div>
              <div><dt>今日浏览量</dt><dd>{{ siteStats.todayViews.toLocaleString() }}</dd></div>
              <div><dt>总浏览量</dt><dd>{{ siteStats.totalViews.toLocaleString() }}</dd></div>
              <div><dt>总访客量</dt><dd>{{ siteStats.totalVisitors.toLocaleString() }}</dd></div>
            </dl>
          </section>

          <section class="home-panel home-calendar-card">
            <div class="calendar-heading">
              <h2>{{ calendarLabel }}</h2>
            </div>
            <div class="calendar-weekdays" aria-hidden="true">
              <span v-for="day in ['日', '一', '二', '三', '四', '五', '六']" :key="day">{{ day }}</span>
            </div>
            <div class="calendar-days">
              <span
                v-for="(day, index) in calendarCells"
                :key="`${calendarLabel}-${index}`"
                :class="{ today: day === calendarToday, empty: day === null }"
                :style="{ '--motion-order': index }"
              >{{ day }}</span>
            </div>
          </section>
        </aside>
      </div>
    </section>
  </div>
</template>
