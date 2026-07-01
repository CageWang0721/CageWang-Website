package com.example.blog.article.dto;

import java.util.List;

public record PublicArchiveMonth(
        int year,
        int month,
        List<PublicArticleCard> articles
) {
}
