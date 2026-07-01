<script setup lang="ts">
import type { ArticleCard } from '~/types/blog'

defineProps<{
  article: ArticleCard
  featured?: boolean
  index?: number
}>()
</script>

<template>
  <article class="article-card" :class="{ featured }">
    <div class="card-top">
      <span class="card-number">{{ String((index ?? 0) + 1).padStart(2, '0') }}</span>
      <span v-if="article.pinned" class="pinned">置顶</span>
    </div>
    <div class="card-body">
      <NuxtLink
        v-if="article.categorySlug"
        class="category-link"
        :to="`/category/${article.categorySlug}`"
      >
        {{ article.categoryName }}
      </NuxtLink>
      <h2>
        <NuxtLink :to="`/article/${article.slug}`">{{ article.title }}</NuxtLink>
      </h2>
      <p>{{ article.summary || '一篇还没来得及写摘要的文章，正文里见。' }}</p>
    </div>
    <div class="card-meta">
      <time :datetime="article.publishedAt">{{ formatDate(article.publishedAt) }}</time>
      <span>{{ article.readingMinutes }} 分钟阅读</span>
      <NuxtLink class="read-more" :to="`/article/${article.slug}`" aria-label="阅读全文">
        阅读
        <span aria-hidden="true">↗</span>
      </NuxtLink>
    </div>
  </article>
</template>
