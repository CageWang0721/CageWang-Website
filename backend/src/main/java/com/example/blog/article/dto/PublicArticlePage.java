package com.example.blog.article.dto;

import java.util.List;

public record PublicArticlePage(
        List<PublicArticleCard> items,
        long total,
        int page,
        int pageSize,
        int totalPages
) {
}
