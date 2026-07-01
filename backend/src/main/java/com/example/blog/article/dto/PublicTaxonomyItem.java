package com.example.blog.article.dto;

public record PublicTaxonomyItem(
        Long id,
        String name,
        String slug,
        String description,
        long articleCount
) {
}
