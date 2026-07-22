package com.example.blog.comment.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.article.service.MarkdownService;
import com.example.blog.comment.dto.PublicCommentResponse;
import com.example.blog.comment.mapper.CommentMapper;
import com.example.blog.comment.model.CommentRecord;
import com.example.blog.operation.service.OperationLogService;
import com.example.blog.shared.error.ApiException;

@Service
public class CommentModerationService {

    private static final String COMMENT_ENTITY = "COMMENT";

    private final CommentMapper comments;
    private final MarkdownService markdown;
    private final OperationLogService operationLogs;
    private final CommentPolicy policy;
    private final CommentResponseMapper responses;
    private final CommentReplyNotifier replyNotifier;

    public CommentModerationService(
            CommentMapper comments,
            MarkdownService markdown,
            OperationLogService operationLogs,
            CommentPolicy policy,
            CommentResponseMapper responses,
            CommentReplyNotifier replyNotifier
    ) {
        this.comments = comments;
        this.markdown = markdown;
        this.operationLogs = operationLogs;
        this.policy = policy;
        this.responses = responses;
        this.replyNotifier = replyNotifier;
    }

    @Transactional
    public void moderate(Long id, String status, Long operatorId) {
        policy.validateModerationStatus(status);
        CommentRecord comment = requireComment(id);

        if (comments.updateStatus(id, status) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "评论不存在");
        }

        refreshArticleCommentCount(comment);
        if ("APPROVED".equals(status)) {
            replyNotifier.notifyParentOfApprovedReply(comment);
        }
        operationLogs.record(
                operatorId,
                COMMENT_ENTITY,
                status,
                id,
                "{\"type\":\"" + comment.type() + "\"}"
        );
    }

    @Transactional
    public void delete(Long id, Long operatorId) {
        CommentRecord comment = requireComment(id);
        if (comments.softDelete(id) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "评论不存在");
        }

        refreshArticleCommentCount(comment);
        operationLogs.record(operatorId, COMMENT_ENTITY, "DELETE", id, "{}");
    }

    @Transactional
    public PublicCommentResponse adminReply(Long id, String content, Long operatorId) {
        CommentRecord parent = requireComment(id);
        String normalizedContent = policy.validateContent(content);
        Long replyId = insertAdminReply(parent, normalizedContent, operatorId);

        refreshArticleCommentCount(parent);
        replyNotifier.notifyReply(parent, normalizedContent);
        operationLogs.record(
                operatorId,
                COMMENT_ENTITY,
                "REPLY",
                replyId,
                "{\"parentId\":" + id + "}"
        );

        return responses.toPublicComment(requireComment(replyId));
    }

    private Long insertAdminReply(CommentRecord parent, String content, Long operatorId) {
        var rendered = markdown.renderComment(content);
        Long rootId = parent.rootId() == null ? parent.id() : parent.rootId();

        comments.insert(
                parent.articleId(),
                rootId,
                rootId,
                parent.type(),
                content,
                rendered.html(),
                "博主",
                null,
                null,
                sha256("admin:" + operatorId),
                "APPROVED",
                true,
                false,
                "0".repeat(64),
                "admin"
        );
        return comments.lastInsertId();
    }

    private void refreshArticleCommentCount(CommentRecord comment) {
        if (comment.articleId() != null) {
            comments.refreshArticleCommentCount(comment.articleId());
        }
    }

    private CommentRecord requireComment(Long id) {
        return comments.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "评论不存在"));
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
