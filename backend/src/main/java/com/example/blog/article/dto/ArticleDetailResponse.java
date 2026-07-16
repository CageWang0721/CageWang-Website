package com.example.blog.article.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.blog.article.model.ArticleRecord;

public record ArticleDetailResponse(
        Long id,
        String title,
        String slug,
        String summary,
        String contentMarkdown,
        String contentHtml,
        Long categoryId,
        List<Long> tagIds,
        String status,
        String visibility,
        boolean pinned,
        boolean allowComment,
        int wordCount,
        int readingMinutes,
        long viewCount,
        String metaTitle,
        String metaDescription,
        String canonicalUrl,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int version
) {
    public static ArticleDetailResponse from(ArticleRecord article, List<Long> tagIds) {
        return new ArticleDetailResponse(
                article.id(),
                article.title(),
                article.slug(),
                article.summary(),
                article.contentMarkdown(),
                article.contentHtml(),
                article.categoryId(),
                tagIds,
                article.status(),
                article.visibility(),
                article.pinned(),
                article.allowComment(),
                article.wordCount(),
                article.readingMinutes(),
                article.viewCount(),
                article.metaTitle(),
                article.metaDescription(),
                article.canonicalUrl(),
                article.publishedAt(),
                article.createdAt(),
                article.updatedAt(),
                article.version()
        );
    }
}
