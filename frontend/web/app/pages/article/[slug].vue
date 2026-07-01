<script setup lang="ts">
const route = useRoute()
const config = useRuntimeConfig()
const api = useBlogApi()
const slug = String(route.params.slug)

const [{ data: article, error }, { data: adjacent }, { data: related }] = await Promise.all([
  useAsyncData(`article-${slug}`, () => api.article(slug)),
  useAsyncData(`article-adjacent-${slug}`, () => api.adjacent(slug)),
  useAsyncData(`article-related-${slug}`, () => api.related(slug))
])

if (error.value) {
  throw createError({ statusCode: 404, statusMessage: '没有找到这篇文章' })
}

const canonical = computed(() =>
  article.value?.canonicalUrl || `${config.public.siteUrl}/article/${slug}`
)

useSeoMeta({
  title: () => article.value?.metaTitle || `${article.value?.title || '文章'} · 余白札记`,
  description: () => article.value?.metaDescription || article.value?.summary || '余白札记文章',
  ogTitle: () => article.value?.metaTitle || article.value?.title,
  ogDescription: () => article.value?.metaDescription || article.value?.summary || undefined,
  ogType: 'article',
  articlePublishedTime: () => article.value?.publishedAt,
  articleModifiedTime: () => article.value?.updatedAt,
  twitterCard: 'summary_large_image'
})
useHead({
  link: [{ rel: 'canonical', href: canonical }],
  script: [{
    type: 'application/ld+json',
    innerHTML: computed(() => JSON.stringify({
      '@context': 'https://schema.org',
      '@type': 'BlogPosting',
      headline: article.value?.title,
      description: article.value?.metaDescription || article.value?.summary,
      datePublished: article.value?.publishedAt,
      dateModified: article.value?.updatedAt,
      mainEntityOfPage: canonical.value,
      author: { '@type': 'Person', name: '余白札记' },
      publisher: { '@type': 'Organization', name: '余白札记' }
    }))
  }]
})
</script>

<template>
  <article v-if="article" class="article-page">
    <header class="article-hero">
      <div class="reading-width">
        <div class="article-kicker">
          <NuxtLink
            v-if="article.categorySlug"
            :to="`/category/${article.categorySlug}`"
          >
            {{ article.categoryName }}
          </NuxtLink>
          <span v-else>未分类</span>
          <span class="kicker-line" />
          <time :datetime="article.publishedAt">{{ formatDate(article.publishedAt) }}</time>
        </div>
        <h1>{{ article.title }}</h1>
        <p v-if="article.summary" class="article-summary">{{ article.summary }}</p>
        <div class="article-stats">
          <span>{{ article.wordCount.toLocaleString() }} 字</span>
          <span>{{ article.readingMinutes }} 分钟阅读</span>
          <span>{{ article.viewCount.toLocaleString() }} 次浏览</span>
        </div>
      </div>
    </header>

    <div class="article-layout content-width">
      <aside class="article-rail" aria-label="文章信息">
        <span>NOTE</span>
        <strong>{{ new Date(article.publishedAt).getFullYear() }}</strong>
        <div class="rail-line" />
      </aside>
      <div class="article-main">
        <!-- HTML 在后端由 Jsoup safelist 清理后输出。 -->
        <div class="prose" v-html="article.contentHtml" />

        <footer class="article-footer">
          <div v-if="article.tags.length" class="article-tags">
            <NuxtLink v-for="tag in article.tags" :key="tag.id" :to="`/tag/${tag.slug}`">
              # {{ tag.name }}
            </NuxtLink>
          </div>
          <p>感谢你读到这里。愿这页文字，恰好对你有一点用。</p>
        </footer>

        <nav v-if="adjacent?.previous || adjacent?.next" class="article-navigation">
          <NuxtLink v-if="adjacent.previous" :to="`/article/${adjacent.previous.slug}`">
            <small>← 上一篇</small>
            <strong>{{ adjacent.previous.title }}</strong>
          </NuxtLink>
          <span v-else />
          <NuxtLink v-if="adjacent.next" class="next" :to="`/article/${adjacent.next.slug}`">
            <small>下一篇 →</small>
            <strong>{{ adjacent.next.title }}</strong>
          </NuxtLink>
        </nav>
      </div>
    </div>

    <section v-if="related?.length" class="related-section">
      <div class="content-width">
        <div class="section-heading">
          <div>
            <p class="eyebrow">KEEP READING</p>
            <h2>也许还会喜欢</h2>
          </div>
        </div>
        <div class="article-grid">
          <ArticleCard
            v-for="(item, index) in related"
            :key="item.id"
            :article="item"
            :index="index"
          />
        </div>
      </div>
    </section>
  </article>
</template>
