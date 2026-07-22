package com.example.blog.article.dto;

import java.time.Instant;

public record ArticlePublishRequest(
        Instant publishedAt
) {
}
