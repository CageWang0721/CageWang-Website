package com.example.blog.article.dto;

import java.time.LocalDateTime;

public record PublicArticleRecord(
        Long id,
        String title,
        String slug,
        String summary,
        String contentHtml,
        String categoryName,
        String categorySlug,
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
