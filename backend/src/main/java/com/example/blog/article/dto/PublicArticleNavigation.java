package com.example.blog.article.dto;

public record PublicArticleNavigation(
        PublicArticleCard previous,
        PublicArticleCard next
) {
}
