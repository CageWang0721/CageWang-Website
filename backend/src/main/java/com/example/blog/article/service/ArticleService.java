package com.example.blog.article.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.article.dto.ArticleDetailResponse;
import com.example.blog.article.dto.ArticlePageResponse;
import com.example.blog.article.dto.ArticleUpsertRequest;
import com.example.blog.article.dto.TaxonomyOption;
import com.example.blog.article.mapper.ArticleMapper;
import com.example.blog.article.model.ArticleRecord;
import com.example.blog.article.service.MarkdownService.RenderedMarkdown;
import com.example.blog.shared.error.ApiException;

@Service
public class ArticleService {

    private static final Set<String> STATUSES =
            Set.of("DRAFT", "SCHEDULED", "PUBLISHED", "ARCHIVED");

    private final ArticleMapper articleMapper;
    private final MarkdownService markdownService;

    public ArticleService(ArticleMapper articleMapper, MarkdownService markdownService) {
        this.articleMapper = articleMapper;
        this.markdownService = markdownService;
    }

    public ArticlePageResponse findPage(String status, String keyword, int page, int pageSize) {
        String normalizedStatus = normalizeStatus(status);
        String normalizedKeyword = blankToNull(keyword);
        int safePage = Math.max(1, page);
        int safePageSize = Math.clamp(pageSize, 1, 100);
        long offset = (long) (safePage - 1) * safePageSize;
        return new ArticlePageResponse(
                articleMapper.findPage(normalizedStatus, normalizedKeyword, offset, safePageSize),
                articleMapper.count(normalizedStatus, normalizedKeyword),
                safePage,
                safePageSize
        );
    }

    public ArticleDetailResponse findById(Long id) {
        ArticleRecord article = requireArticle(id);
        return ArticleDetailResponse.from(article, articleMapper.findTagIds(id));
    }

    public List<TaxonomyOption> findCategories() {
        return articleMapper.findCategories();
    }

    public List<TaxonomyOption> findTags() {
        return articleMapper.findTags();
    }

    @Transactional
    public ArticleDetailResponse create(ArticleUpsertRequest request) {
        PreparedArticle article = prepareArticle(request, null);
        articleMapper.insert(
                article.title(),
                article.slug(),
                article.summary(),
                article.markdown(),
                article.rendered().html(),
                article.rendered().plain(),
                article.categoryId(),
                article.visibility(),
                article.pinned(),
                article.allowComment(),
                article.rendered().wordCount(),
                article.rendered().readingMinutes(),
                article.metaTitle(),
                article.metaDescription(),
                article.canonicalUrl()
        );
        Long id = articleMapper.lastInsertId();
        replaceTags(id, request.tagIds());
        return findById(id);
    }

    @Transactional
    public ArticleDetailResponse update(Long id, ArticleUpsertRequest request) {
        ArticleRecord existing = requireArticle(id);
        PreparedArticle article = prepareArticle(request, id);
        int changed = articleMapper.update(
                id,
                article.title(),
                article.slug(),
                article.summary(),
                article.markdown(),
                article.rendered().html(),
                article.rendered().plain(),
                article.categoryId(),
                article.visibility(),
                article.pinned(),
                article.allowComment(),
                article.rendered().wordCount(),
                article.rendered().readingMinutes(),
                article.metaTitle(),
                article.metaDescription(),
                article.canonicalUrl(),
                request.version()
        );
        if (changed == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "文章已被其他操作修改，请刷新后重试");
        }
        if (!existing.slug().equals(article.slug())) {
            articleMapper.insertRedirect(existing.slug(), "/article/" + article.slug());
        }
        replaceTags(id, request.tagIds());
        return findById(id);
    }

    @Transactional
    public ArticleDetailResponse publish(Long id, Instant requestedPublishedAt) {
        ArticleRecord article = requireArticle(id);
        validatePublishable(article);
        Instant now = Instant.now();
        Instant publishInstant = requestedPublishedAt == null ? now : requestedPublishedAt;
        LocalDateTime publishedAt = LocalDateTime.ofInstant(publishInstant, ZoneOffset.UTC);
        boolean scheduled = publishInstant.isAfter(now);
        if (articleMapper.publish(
                id,
                scheduled ? "SCHEDULED" : "PUBLISHED",
                publishedAt,
                scheduled ? publishedAt : null
        ) == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "文章当前状态不能发布");
        }
        return findById(id);
    }

    @Transactional
    public ArticleDetailResponse withdraw(Long id) {
        requireArticle(id);
        if (articleMapper.withdraw(id) == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "只有已发布文章可以撤回");
        }
        return findById(id);
    }

    @Transactional
    public void delete(Long id) {
        requireArticle(id);
        if (articleMapper.softDelete(id) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "文章不存在");
        }
    }

    private ArticleRecord requireArticle(Long id) {
        return articleMapper.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "文章不存在"));
    }

    private void validateReferences(ArticleUpsertRequest request) {
        if (request.categoryId() != null && !articleMapper.categoryExists(request.categoryId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "所选分类不存在");
        }
        if (!request.tagIds().isEmpty()
                && articleMapper.countExistingTags(request.tagIds()) != request.tagIds().size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "所选标签包含无效项");
        }
    }

    private void validatePublishable(ArticleRecord article) {
        if (article.title() == null || article.title().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "标题不能为空，无法发布");
        }
        if (article.slug() == null || article.slug().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "slug 不能为空，无法发布");
        }
    }

    private void ensureSlugAvailable(String slug, Long excludeId) {
        if (articleMapper.slugExists(slug, excludeId)) {
            throw new ApiException(HttpStatus.CONFLICT, "文章 slug 已存在");
        }
    }

    private void replaceTags(Long articleId, Set<Long> tagIds) {
        articleMapper.deleteTags(articleId);
        if (!tagIds.isEmpty()) {
            articleMapper.insertTags(articleId, tagIds);
        }
    }

    private PreparedArticle prepareArticle(ArticleUpsertRequest request, Long articleId) {
        validateReferences(request);
        String slug = request.slug().trim();
        ensureSlugAvailable(slug, articleId);
        return new PreparedArticle(
                request.title().trim(),
                slug,
                blankToNull(request.summary()),
                request.contentMarkdown(),
                markdownService.renderArticle(request.contentMarkdown()),
                request.categoryId(),
                request.visibility(),
                request.pinned(),
                request.allowComment(),
                blankToNull(request.metaTitle()),
                blankToNull(request.metaDescription()),
                blankToNull(request.canonicalUrl())
        );
    }

    private String normalizeStatus(String status) {
        String normalized = blankToNull(status);
        if (normalized == null) return null;
        normalized = normalized.toUpperCase();
        if (!STATUSES.contains(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "无效的文章状态");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private record PreparedArticle(
            String title,
            String slug,
            String summary,
            String markdown,
            RenderedMarkdown rendered,
            Long categoryId,
            String visibility,
            boolean pinned,
            boolean allowComment,
            String metaTitle,
            String metaDescription,
            String canonicalUrl
    ) {
    }
}
