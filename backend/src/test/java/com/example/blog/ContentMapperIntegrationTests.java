package com.example.blog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.article.mapper.ArticleMapper;
import com.example.blog.article.mapper.PublicArticleMapper;
import com.example.blog.comment.mapper.CommentMapper;
import com.example.blog.media.mapper.MediaMapper;
import com.example.blog.taxonomy.mapper.TaxonomyMapper;

@SpringBootTest
@Transactional
class ContentMapperIntegrationTests extends ContainerIntegrationTest {

    @Autowired
    private TaxonomyMapper taxonomyMapper;

    @Autowired
    private MediaMapper mediaMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private PublicArticleMapper publicArticleMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Test
    void persistsAndReadsCategoryAndTag() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        taxonomyMapper.insertCategory("Category " + suffix, "category-" + suffix, null, 2, true);
        Long categoryId = taxonomyMapper.lastInsertId();
        taxonomyMapper.insertTag("Tag " + suffix, "tag-" + suffix, "description", 3, true);
        Long tagId = taxonomyMapper.lastInsertId();

        var category = taxonomyMapper.findCategoryById(categoryId).orElseThrow();
        var tag = taxonomyMapper.findTagById(tagId).orElseThrow();

        assertEquals(0, category.articleCount());
        assertEquals(0, tag.articleCount());
        assertTrue(category.visible());
        assertTrue(tag.visible());
    }

    @Test
    void persistsAndReadsMediaMetadata() {
        String objectKey = "blog/test/" + UUID.randomUUID() + ".png";
        mediaMapper.insert(
                objectKey,
                "test.png",
                "image/png",
                "png",
                128,
                16,
                8,
                "0".repeat(64),
                "test image",
                null
        );
        Long id = mediaMapper.lastInsertId();

        var media = mediaMapper.findById(id).orElseThrow();

        assertEquals(objectKey, media.objectKey());
        assertEquals(0, media.referenceCount());
        assertEquals("PENDING", media.status());
    }

    @Test
    void reconcilesMediaReferencesAndMarksOrphans() {
        String objectKey = "blog/test/" + UUID.randomUUID() + ".png";
        mediaMapper.insert(
                objectKey,
                "lifecycle.png",
                "image/png",
                "png",
                128,
                16,
                8,
                "1".repeat(64),
                "lifecycle image",
                null
        );
        Long mediaId = mediaMapper.lastInsertId();
        String slug = "media-lifecycle-" + UUID.randomUUID().toString().substring(0, 8);
        articleMapper.insert(
                "Media lifecycle",
                slug,
                null,
                "![image](https://cdn.example.com/" + objectKey + ")",
                "<p>image</p>",
                "image",
                null,
                "PUBLIC",
                false,
                true,
                1,
                1,
                null,
                null,
                null
        );
        Long articleId = articleMapper.lastInsertId();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        mediaMapper.reconcileReferences(now);

        var active = mediaMapper.findById(mediaId).orElseThrow();
        assertEquals("ACTIVE", active.status());
        assertEquals(1, active.referenceCount());
        assertNull(active.unreferencedAt());

        articleMapper.softDelete(articleId);
        mediaMapper.reconcileReferences(now.plusMinutes(1));
        mediaMapper.markOrphans(now.plusMinutes(2));

        var orphan = mediaMapper.findById(mediaId).orElseThrow();
        assertEquals("ORPHAN", orphan.status());
        assertEquals(0, orphan.referenceCount());
        assertTrue(mediaMapper.findDeleteCandidates(now.plusMinutes(2), 10)
                .stream()
                .anyMatch(item -> item.id().equals(mediaId)));
    }

    @Test
    void persistsAndReadsArticleRecordAndListProjection() {
        String slug = "integration-" + UUID.randomUUID().toString().substring(0, 8);
        articleMapper.insert(
                "Integration article",
                slug,
                null,
                "# Integration",
                "<h1>Integration</h1>",
                "Integration",
                null,
                "PUBLIC",
                false,
                true,
                1,
                1,
                null,
                null,
                null
        );
        Long id = articleMapper.lastInsertId();

        var detail = articleMapper.findById(id).orElseThrow();
        var page = articleMapper.findPage("DRAFT", "Integration article", 0, 20);

        assertEquals(slug, detail.slug());
        assertTrue(page.stream().anyMatch(item -> item.id().equals(id)));
    }

    @Test
    void onlyExposesPublishedPublicArticles() {
        String slug = "public-" + UUID.randomUUID().toString().substring(0, 8);
        articleMapper.insert(
                "Public article", slug, "Visible summary", "# Public",
                "<h1>Public</h1>", "Public", null, "PUBLIC", true, true,
                1, 1, null, null, null
        );
        Long id = articleMapper.lastInsertId();

        assertTrue(publicArticleMapper.findBySlug(slug).isEmpty());
        articleMapper.publish(id, java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));

        var publicArticle = publicArticleMapper.findBySlug(slug).orElseThrow();
        assertEquals("Public article", publicArticle.title());
        assertTrue(publicArticleMapper.findArticles(null, null, null, 0, 20)
                .stream().anyMatch(item -> item.id().equals(id)));
    }

    @Test
    void persistsMapsAndSoftDeletesACommentThread() {
        String hash = "a".repeat(64);
        commentMapper.insert(
                null, null, null, "MESSAGE", "Root", "<p>Root</p>",
                "Reader", null, null, hash, "APPROVED", false, false,
                "b".repeat(64), "integration-test"
        );
        Long rootId = commentMapper.lastInsertId();
        commentMapper.insert(
                null, rootId, rootId, "MESSAGE", "Reply", "<p>Reply</p>",
                "Author", null, null, "c".repeat(64), "APPROVED", true, false,
                "d".repeat(64), "integration-test"
        );
        Long replyId = commentMapper.lastInsertId();

        assertEquals("Reader", commentMapper.findById(rootId).orElseThrow().nickname());
        assertEquals(rootId, commentMapper.findById(replyId).orElseThrow().rootId());

        commentMapper.softDelete(rootId);

        assertTrue(commentMapper.findById(rootId).isEmpty());
        assertTrue(commentMapper.findById(replyId).isEmpty());
    }
}
