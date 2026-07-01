package com.example.blog.article.dto;

import java.time.LocalDateTime;

public record PublicArticleCard(
        Long id,
        String title,
        String slug,
        String summary,
        String categoryName,
        String categorySlug,
        boolean pinned,
        int readingMinutes,
        long viewCount,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
}
