package com.example.blog.article.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.blog.article.dto.PublicArticleCard;
import com.example.blog.article.dto.PublicArticleRecord;
import com.example.blog.article.dto.PublicTaxonomyItem;

@Mapper
public interface PublicArticleMapper {

    List<PublicArticleCard> findArticles(
            @Param("keyword") String keyword,
            @Param("categorySlug") String categorySlug,
            @Param("tagSlug") String tagSlug,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    long countArticles(
            @Param("keyword") String keyword,
            @Param("categorySlug") String categorySlug,
            @Param("tagSlug") String tagSlug
    );

    Optional<PublicArticleRecord> findBySlug(String slug);

    List<PublicTaxonomyItem> findArticleTags(Long articleId);

    List<PublicTaxonomyItem> findCategories();

    List<PublicTaxonomyItem> findTags();

    Optional<PublicArticleCard> findPrevious(
            @Param("publishedAt") java.time.LocalDateTime publishedAt,
            @Param("id") Long id
    );

    Optional<PublicArticleCard> findNext(
            @Param("publishedAt") java.time.LocalDateTime publishedAt,
            @Param("id") Long id
    );

    List<PublicArticleCard> findRelated(
            @Param("articleId") Long articleId,
            @Param("categorySlug") String categorySlug,
            @Param("limit") int limit
    );

    Optional<String> findRedirect(String oldSlug);
}
