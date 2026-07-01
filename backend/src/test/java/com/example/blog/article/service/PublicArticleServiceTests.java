package com.example.blog.article.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.blog.article.dto.PublicArticleRecord;
import com.example.blog.article.dto.PublicTaxonomyItem;
import com.example.blog.article.mapper.PublicArticleMapper;
import com.example.blog.shared.error.ApiException;

@ExtendWith(MockitoExtension.class)
class PublicArticleServiceTests {

    @Mock
    private PublicArticleMapper mapper;

    private PublicArticleService service;

    @BeforeEach
    void setUp() {
        service = new PublicArticleService(mapper);
    }

    @Test
    void clampsPaginationBeforeQuerying() {
        when(mapper.countArticles(null, null, null)).thenReturn(120L);

        var page = service.findArticles(null, null, null, -2, 1000);

        assertEquals(1, page.page());
        assertEquals(50, page.pageSize());
        assertEquals(3, page.totalPages());
        verify(mapper).findArticles(null, null, null, 0, 50);
    }

    @Test
    void attachesVisibleTagsToArticleDetail() {
        when(mapper.findBySlug("hello")).thenReturn(Optional.of(article()));
        when(mapper.findArticleTags(7L)).thenReturn(List.of(
                new PublicTaxonomyItem(2L, "Java", "java", null, 0)
        ));

        var detail = service.findBySlug("hello");

        assertEquals("Java", detail.tags().getFirst().name());
    }

    @Test
    void rejectsOverlongSearchKeyword() {
        assertThrows(
                ApiException.class,
                () -> service.findArticles("x".repeat(101), null, null, 1, 12)
        );
    }

    private PublicArticleRecord article() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 8, 0);
        return new PublicArticleRecord(
                7L, "Hello", "hello", "Summary", "<p>Hello</p>",
                "Notes", "notes", false, true, 10, 1, 0,
                null, null, null, now, now
        );
    }
}
