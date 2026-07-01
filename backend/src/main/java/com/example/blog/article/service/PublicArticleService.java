package com.example.blog.article.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.blog.article.dto.PublicArchiveMonth;
import com.example.blog.article.dto.PublicArticleCard;
import com.example.blog.article.dto.PublicArticleDetail;
import com.example.blog.article.dto.PublicArticleNavigation;
import com.example.blog.article.dto.PublicArticlePage;
import com.example.blog.article.dto.PublicArticleRecord;
import com.example.blog.article.dto.PublicHomeResponse;
import com.example.blog.article.dto.PublicTaxonomyItem;
import com.example.blog.article.mapper.PublicArticleMapper;
import com.example.blog.shared.error.ApiException;

@Service
public class PublicArticleService {

    private final PublicArticleMapper mapper;

    public PublicArticleService(PublicArticleMapper mapper) {
        this.mapper = mapper;
    }

    public PublicArticlePage findArticles(
            String keyword,
            String categorySlug,
            String tagSlug,
            int page,
            int pageSize
    ) {
        String safeKeyword = normalizeKeyword(keyword);
        String safeCategory = blankToNull(categorySlug);
        String safeTag = blankToNull(tagSlug);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.clamp(pageSize, 1, 50);
        long total = mapper.countArticles(safeKeyword, safeCategory, safeTag);
        long offset = (long) (safePage - 1) * safePageSize;
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safePageSize);
        return new PublicArticlePage(
                mapper.findArticles(safeKeyword, safeCategory, safeTag, offset, safePageSize),
                total,
                safePage,
                safePageSize,
                totalPages
        );
    }

    public PublicArticleDetail findBySlug(String slug) {
        PublicArticleRecord article = mapper.findBySlug(slug)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "文章不存在"));
        return withTags(article);
    }

    public Optional<String> findRedirect(String slug) {
        return mapper.findRedirect(slug);
    }

    public PublicHomeResponse home() {
        PublicArticlePage page = findArticles(null, null, null, 1, 9);
        List<PublicArticleCard> featured = page.items().stream()
                .filter(PublicArticleCard::pinned)
                .limit(3)
                .toList();
        if (featured.isEmpty()) {
            featured = page.items().stream().limit(3).toList();
        }
        return new PublicHomeResponse(
                featured,
                page.items(),
                mapper.findCategories(),
                mapper.findTags(),
                page.total()
        );
    }

    public List<PublicTaxonomyItem> categories() {
        return mapper.findCategories();
    }

    public List<PublicTaxonomyItem> tags() {
        return mapper.findTags();
    }

    public List<PublicArchiveMonth> archives() {
        List<PublicArticleCard> articles = mapper.findArticles(null, null, null, 0, 1000);
        Map<String, List<PublicArticleCard>> groups = new LinkedHashMap<>();
        for (PublicArticleCard article : articles) {
            String key = article.publishedAt().getYear() + "-" + article.publishedAt().getMonthValue();
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(article);
        }
        return groups.values().stream()
                .map(items -> new PublicArchiveMonth(
                        items.getFirst().publishedAt().getYear(),
                        items.getFirst().publishedAt().getMonthValue(),
                        List.copyOf(items)))
                .toList();
    }

    public PublicArticleNavigation adjacent(String slug) {
        PublicArticleDetail article = findBySlug(slug);
        return new PublicArticleNavigation(
                mapper.findPrevious(article.publishedAt(), article.id()).orElse(null),
                mapper.findNext(article.publishedAt(), article.id()).orElse(null)
        );
    }

    public List<PublicArticleCard> related(String slug) {
        PublicArticleDetail article = findBySlug(slug);
        return mapper.findRelated(article.id(), article.categorySlug(), 3);
    }

    private PublicArticleDetail withTags(PublicArticleRecord article) {
        return new PublicArticleDetail(
                article.id(), article.title(), article.slug(), article.summary(),
                article.contentHtml(), article.categoryName(), article.categorySlug(),
                mapper.findArticleTags(article.id()), article.pinned(), article.allowComment(), article.wordCount(),
                article.readingMinutes(), article.viewCount(), article.metaTitle(),
                article.metaDescription(), article.canonicalUrl(), article.publishedAt(),
                article.updatedAt()
        );
    }

    private String normalizeKeyword(String keyword) {
        String normalized = blankToNull(keyword);
        if (normalized != null && normalized.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "搜索关键词不能超过 100 个字符");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
