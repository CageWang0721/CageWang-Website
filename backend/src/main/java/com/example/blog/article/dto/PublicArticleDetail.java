package com.example.blog.article.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PublicArticleDetail(
        Long id,
        String title,
        String slug,
        String summary,
        String contentHtml,
        String categoryName,
        String categorySlug,
        List<PublicTaxonomyItem> tags,
        boolean pinned,
        boolean allowComment,
        int wordCount,
        int readingMinutes,
        long viewCount,
        String metaTitle,
        String metaDescription,
        String canonicalUrl,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
}
