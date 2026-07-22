package com.example.blog.article.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.blog.article.mapper.ArticleMapper;
import com.example.blog.article.model.ArticleRecord;
import com.example.blog.shared.cache.PublicContentCache;

class ArticlePublicationTests {

    @Test
    void pastCustomTimePublishesImmediately() {
        ArticleMapper mapper = preparedMapper();
        ArticleService service = new ArticleService(mapper, mock(MarkdownService.class));
        Instant requested = Instant.parse("2024-05-01T04:30:00Z");
        LocalDateTime utcTime = LocalDateTime.ofInstant(requested, ZoneOffset.UTC);

        service.publish(1L, requested);

        verify(mapper).publish(1L, "PUBLISHED", utcTime, null);
    }

    @Test
    void futureCustomTimeCreatesSchedule() {
        ArticleMapper mapper = preparedMapper();
        ArticleService service = new ArticleService(mapper, mock(MarkdownService.class));
        Instant requested = Instant.now().plus(Duration.ofDays(7));
        LocalDateTime utcTime = LocalDateTime.ofInstant(requested, ZoneOffset.UTC);

        service.publish(1L, requested);

        verify(mapper).publish(1L, "SCHEDULED", utcTime, utcTime);
    }

    @Test
    void schedulerInvalidatesPublicCacheOnlyWhenArticlesBecomeDue() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        PublicContentCache cache = mock(PublicContentCache.class);
        ArticlePublicationScheduler scheduler = new ArticlePublicationScheduler(mapper, cache);
        when(mapper.publishDueScheduled(org.mockito.ArgumentMatchers.any())).thenReturn(0, 2);

        scheduler.publishDueArticles();
        verify(cache, never()).invalidateAll();

        scheduler.publishDueArticles();
        verify(cache).invalidateAll();
    }

    private ArticleMapper preparedMapper() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.findById(1L)).thenReturn(Optional.of(new ArticleRecord(
                1L,
                "测试文章",
                "test-article",
                "摘要",
                "正文",
                "<p>正文</p>",
                null,
                "DRAFT",
                "PUBLIC",
                false,
                true,
                2,
                1,
                0,
                null,
                null,
                null,
                null,
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC),
                0
        )));
        when(mapper.publish(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.nullable(LocalDateTime.class)
        )).thenReturn(1);
        when(mapper.findTagIds(1L)).thenReturn(List.of());
        return mapper;
    }
}
