export interface ArticleCard {
  id: number
  title: string
  slug: string
  summary: string | null
  categoryName: string | null
  categorySlug: string | null
  pinned: boolean
  readingMinutes: number
  viewCount: number
  publishedAt: string
  updatedAt: string
}

export interface TaxonomyItem {
  id: number
  name: string
  slug: string
  description: string | null
  articleCount: number
}

export interface ArticlePage {
  items: ArticleCard[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export interface ArticleDetail {
  id: number
  title: string
  slug: string
  summary: string | null
  contentHtml: string
  categoryName: string | null
  categorySlug: string | null
  tags: TaxonomyItem[]
  pinned: boolean
  allowComment: boolean
  wordCount: number
  readingMinutes: number
  viewCount: number
  metaTitle: string | null
  metaDescription: string | null
  canonicalUrl: string | null
  publishedAt: string
  updatedAt: string
}

export interface HomeResponse {
  featured: ArticleCard[]
  latest: ArticleCard[]
  categories: TaxonomyItem[]
  tags: TaxonomyItem[]
  articleCount: number
}

export interface ArchiveMonth {
  year: number
  month: number
  articles: ArticleCard[]
}

export interface ArticleNavigation {
  previous: ArticleCard | null
  next: ArticleCard | null
}
