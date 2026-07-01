package com.example.blog.article.dto;

import java.util.List;

public record PublicHomeResponse(
        List<PublicArticleCard> featured,
        List<PublicArticleCard> latest,
        List<PublicTaxonomyItem> categories,
        List<PublicTaxonomyItem> tags,
        long articleCount
) {
}
