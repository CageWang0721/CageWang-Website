package com.example.blog.article.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.blog.article.mapper.ArticleMapper;
import com.example.blog.shared.cache.PublicContentCache;

@Service
public class ArticlePublicationScheduler {

    private final ArticleMapper articleMapper;
    private final PublicContentCache publicCache;

    public ArticlePublicationScheduler(
            ArticleMapper articleMapper,
            PublicContentCache publicCache
    ) {
        this.articleMapper = articleMapper;
        this.publicCache = publicCache;
    }

    @Scheduled(fixedDelayString = "${blog.article-publication.poll-interval:5000}")
    public void publishDueArticles() {
        int published = articleMapper.publishDueScheduled(LocalDateTime.now(ZoneOffset.UTC));
        if (published > 0) {
            publicCache.invalidateAll();
        }
    }
}
