<script setup lang="ts">
const api = useBlogApi()
const { data, error } = await useAsyncData('home', () => api.home())

useSeoMeta({
  title: '余白札记 · 记录技术、生活与灵光',
  description: '在代码、生活和那些尚未命名的念头之间，留一点呼吸。',
  ogTitle: '余白札记',
  ogDescription: '记录技术、生活与灵光的个人博客。',
  ogType: 'website',
  twitterCard: 'summary_large_image'
})
</script>

<template>
  <div>
    <section class="home-hero">
      <div class="hero-orbit orbit-one" />
      <div class="hero-orbit orbit-two" />
      <div class="hero-content content-width">
        <p class="eyebrow">PERSONAL NOTES · 自在生长</p>
        <h1>在喧闹世界里，<br><em>留一页余白。</em></h1>
        <p class="hero-lead">
          写代码，也写沿途的风。这里收集技术思考、生活观察，以及偶尔撞进脑海的微小灵光。
        </p>
        <div class="hero-actions">
          <NuxtLink class="button primary" to="/blog">翻阅文章 <span>↗</span></NuxtLink>
          <NuxtLink class="text-link" to="/archive">从时间里寻找 <span>→</span></NuxtLink>
        </div>
      </div>
      <div class="hero-note">
        <span>{{ data?.articleCount || '—' }}</span>
        <small>篇公开札记</small>
      </div>
    </section>

    <section class="section content-width">
      <div class="section-heading">
        <div>
          <p class="eyebrow">LATEST NOTES</p>
          <h2>最近写下的</h2>
        </div>
        <NuxtLink class="text-link" to="/blog">查看全部 <span>→</span></NuxtLink>
      </div>

      <div v-if="data?.latest.length" class="article-grid">
        <ArticleCard
          v-for="(article, index) in data.latest.slice(0, 6)"
          :key="article.id"
          :article="article"
          :index="index"
          :featured="index === 0"
        />
      </div>
      <EmptyState
        v-else
        :description="error ? '暂时无法连接内容服务，请稍后再试。' : undefined"
      />
    </section>

    <section v-if="data?.categories.length || data?.tags.length" class="discovery-section">
      <div class="content-width discovery-grid">
        <div>
          <p class="eyebrow">BY SUBJECT</p>
          <h2>循着主题，慢慢逛</h2>
          <p>分类是书架，标签是藏在页边的小路。</p>
        </div>
        <div class="taxonomy-cloud">
          <NuxtLink
            v-for="category in data.categories"
            :key="`category-${category.id}`"
            :to="`/category/${category.slug}`"
          >
            {{ category.name }} <sup>{{ category.articleCount }}</sup>
          </NuxtLink>
          <NuxtLink
            v-for="tag in data.tags.slice(0, 8)"
            :key="`tag-${tag.id}`"
            class="tag-pill"
            :to="`/tag/${tag.slug}`"
          >
            # {{ tag.name }}
          </NuxtLink>
        </div>
      </div>
    </section>
  </div>
</template>
