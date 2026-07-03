package com.example.blog.comment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.blog.article.service.MarkdownService;
import com.example.blog.comment.dto.CommentRequest;
import com.example.blog.comment.mapper.CommentMapper;
import com.example.blog.comment.model.CommentRecord;
import com.example.blog.interaction.service.PublicInteractionGuard;
import com.example.blog.interaction.service.VisitorContext.Visitor;
import com.example.blog.notification.config.MailNotificationProperties;
import com.example.blog.notification.service.NotificationService;
import com.example.blog.notification.service.SensitiveDataCipher;
import com.example.blog.operation.service.OperationLogService;
import com.example.blog.shared.error.ApiException;
import com.example.blog.shared.security.CaptchaService;

@ExtendWith(MockitoExtension.class)
class CommentServiceTests {

    @Mock private CommentMapper mapper;
    @Mock private SensitiveDataCipher cipher;
    @Mock private PublicInteractionGuard guard;
    @Mock private NotificationService notifications;
    @Mock private OperationLogService operationLogs;
    @Mock private CaptchaService captchas;

    private CommentService service;

    @BeforeEach
    void setUp() {
        service = new CommentService(
                mapper,
                new MarkdownService(),
                cipher,
                guard,
                notifications,
                new MailNotificationProperties(false, null, "http://localhost", 5),
                operationLogs,
                captchas
        );
    }

    @Test
    void submitsNewArticleCommentForModeration() {
        when(mapper.articleAcceptsComments(7L)).thenReturn(true);
        when(mapper.lastInsertId()).thenReturn(19L);

        var result = service.submitArticle(
                7L,
                new CommentRequest(
                        "访客", null, null, "写得很好", null, false,
                        "challenge", "4"
                ),
                visitor()
        );

        assertEquals("PENDING", result.status());
        assertEquals(19L, result.id());
        verify(guard).comment(visitor().anonymousKeyHash(), false);
        verify(captchas).verify("challenge", "4");
    }

    @Test
    void buildsOnlyTwoLevelsInPublicTree() {
        CommentRecord root = record(1L, null, null, "读者");
        CommentRecord reply = record(2L, 1L, 1L, "博主");
        when(mapper.findApprovedArticleComments(7L)).thenReturn(List.of(root, reply));

        var tree = service.articleComments(7L);

        assertEquals(1, tree.size());
        assertEquals(1, tree.getFirst().replies().size());
        assertEquals("博主", tree.getFirst().replies().getFirst().nickname());
    }

    @Test
    void rejectsArticleThatDoesNotAcceptComments() {
        when(mapper.articleAcceptsComments(7L)).thenReturn(false);

        assertThrows(
                ApiException.class,
                () -> service.submitArticle(
                        7L,
                        new CommentRequest(
                                "访客", null, null, "内容", null, false,
                                "challenge", "4"
                        ),
                        visitor()
                )
        );
    }

    private Visitor visitor() {
        return new Visitor("a".repeat(64), "b".repeat(64), "test");
    }

    private CommentRecord record(Long id, Long rootId, Long parentId, String nickname) {
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 12, 0);
        return new CommentRecord(
                id, 7L, rootId, parentId, "ARTICLE", "内容", "<p>内容</p>",
                nickname, null, null, "a".repeat(64), "APPROVED",
                "博主".equals(nickname), false, "b".repeat(64), "test", now, now
        );
    }
}
