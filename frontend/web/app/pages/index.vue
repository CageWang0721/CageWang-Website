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
const config = useRuntimeConfig()
const avatarSrc = '/images/wineclouds-avatar.png'
const siteStats = useSiteStatistics()

const socialLinks: Array<{ label: string, href: string, icon: SimpleIcon }> = [
  { label: 'GitHub', href: config.public.socialGithubUrl || '#', icon: siGithub },
  {
    label: '抖音',
    href: config.public.socialDouyinUrl || '#',
    icon: siTiktok
  },
  { label: 'B站', href: config.public.socialBilibiliUrl || '#', icon: siBilibili },
  {
    label: '小红书',
    href: config.public.socialXiaohongshuUrl || '#',
    icon: siXiaohongshu
  }
]

const handleSocialLinkClick = (event: MouseEvent, href: string) => {
  if (href === '#') {
    event.preventDefault()
  }
}

const refreshHome = () => {
  void refresh()
}

onMounted(() => {
  window.addEventListener('focus', refreshHome)
})

onBeforeUnmount(() => {
  window.removeEventListener('focus', refreshHome)
})

const shownArticles = computed(() => data.value?.latest.slice(0, 8) || [])
const categoryCount = computed(() => data.value?.categories.length || 0)
const tagCount = computed(() => data.value?.tags.length || 0)

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
  <div class="reference-home">
    <section class="home-feed-section">
      <div class="home-feed-layout">
        <div class="home-article-column">
          <template v-if="shownArticles.length">
            <ArticleCard
              v-for="(article, index) in shownArticles"
              :key="article.id"
              :article="article"
              :index="index"
            />
          </template>

          <EmptyState
            v-else
            :description="error ? '暂时无法连接内容服务，请稍后再试。' : undefined"
          />
        </div>

        <aside class="home-sidebar">
          <section class="profile-card">
            <img
              class="profile-avatar"
              :src="avatarSrc"
              alt="Wineclouds 的头像"
            >
            <h2>Wineclouds</h2>
            <p>本质哈基米</p>
            <span class="profile-location">
              <i class="iconfont icon-position" aria-hidden="true" />
              中国 · 上海
            </span>

            <div class="profile-counts">
              <NuxtLink to="/blog">
                <strong>{{ data?.articleCount || 0 }}</strong>
                <span>文章</span>
              </NuxtLink>
              <NuxtLink to="/category">
                <strong>{{ categoryCount }}</strong>
                <span>分类</span>
              </NuxtLink>
              <NuxtLink to="/tag">
                <strong>{{ tagCount }}</strong>
                <span>标签</span>
              </NuxtLink>
            </div>

            <div class="profile-links">
              <a
                v-for="link in socialLinks"
                :key="link.label"
                :href="link.href"
                :aria-label="link.label"
                :aria-disabled="link.href === '#'"
                :title="link.label"
                target="_blank"
                rel="noopener noreferrer"
                @click="handleSocialLinkClick($event, link.href)"
              >
                <svg
                  class="profile-social-icon"
                  viewBox="0 0 24 24"
                  aria-hidden="true"
                  focusable="false"
                >
                  <path :d="link.icon.path" fill="currentColor" />
                </svg>
              </a>
            </div>
          </section>

          <section class="site-stat-card">
            <h2>
              <i class="iconfont icon-eye" aria-hidden="true" />
              站点统计
            </h2>
            <dl>
              <div><dt>在线访客</dt><dd>{{ siteStats.onlineVisitors.toLocaleString() }}</dd></div>
              <div><dt>今日浏览量</dt><dd>{{ siteStats.todayViews.toLocaleString() }}</dd></div>
              <div><dt>总浏览量</dt><dd>{{ siteStats.totalViews.toLocaleString() }}</dd></div>
              <div><dt>总访客量</dt><dd>{{ siteStats.totalVisitors.toLocaleString() }}</dd></div>
            </dl>
          </section>
        </aside>
      </div>
    </section>
  </div>
</template>
