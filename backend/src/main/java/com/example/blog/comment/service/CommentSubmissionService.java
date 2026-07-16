package com.example.blog.comment.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.article.service.MarkdownService;
import com.example.blog.comment.dto.CommentRequest;
import com.example.blog.comment.dto.CommentSubmitResponse;
import com.example.blog.comment.mapper.CommentMapper;
import com.example.blog.comment.model.CommentRecord;
import com.example.blog.interaction.service.PublicInteractionGuard;
import com.example.blog.interaction.service.VisitorContext.Visitor;
import com.example.blog.notification.service.SensitiveDataCipher;
import com.example.blog.shared.error.ApiException;
import com.example.blog.shared.security.CaptchaService;

@Service
public class CommentSubmissionService {

    private static final String ARTICLE_TYPE = "ARTICLE";
    private static final String MESSAGE_TYPE = "MESSAGE";
    private static final String PENDING_STATUS = "PENDING";

    private final CommentMapper comments;
    private final MarkdownService markdown;
    private final SensitiveDataCipher cipher;
    private final PublicInteractionGuard interactionGuard;
    private final CaptchaService captchas;
    private final CommentPolicy policy;

    public CommentSubmissionService(
            CommentMapper comments,
            MarkdownService markdown,
            SensitiveDataCipher cipher,
            PublicInteractionGuard interactionGuard,
            CaptchaService captchas,
            CommentPolicy policy
    ) {
        this.comments = comments;
        this.markdown = markdown;
        this.cipher = cipher;
        this.interactionGuard = interactionGuard;
        this.captchas = captchas;
        this.policy = policy;
    }

    @Transactional
    public CommentSubmitResponse submitArticle(
            Long articleId,
            CommentRequest request,
            Visitor visitor
    ) {
        if (!comments.articleAcceptsComments(articleId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "文章不存在或未开放评论");
        }
        return submit(articleId, ARTICLE_TYPE, request, visitor);
    }

    @Transactional
    public CommentSubmitResponse submitMessage(CommentRequest request, Visitor visitor) {
        return submit(null, MESSAGE_TYPE, request, visitor);
    }

    private CommentSubmitResponse submit(
            Long articleId,
            String type,
            CommentRequest request,
            Visitor visitor
    ) {
        interactionGuard.comment(visitor.anonymousKeyHash(), MESSAGE_TYPE.equals(type));

        String content = policy.validateContent(request.content());
        verifyCaptchaWhenRequired(visitor, request, content);
        rejectDuplicateSubmission(visitor, content);

        ReplyTarget replyTarget = resolveReplyTarget(articleId, type, request.parentId());
        persistPendingComment(articleId, type, request, visitor, content, replyTarget);

        return new CommentSubmitResponse(
                comments.lastInsertId(),
                PENDING_STATUS,
                "已提交，审核通过后会公开显示"
        );
    }

    private void verifyCaptchaWhenRequired(
            Visitor visitor,
            CommentRequest request,
            String content
    ) {
        boolean firstInteraction = comments.countByAnonymousKey(visitor.anonymousKeyHash()) == 0;
        if (policy.requiresCaptcha(firstInteraction, content)) {
            captchas.verify(request.captchaId(), request.captchaAnswer());
        }
    }

    private void rejectDuplicateSubmission(Visitor visitor, String content) {
        int duplicateCount = comments.countDuplicate(
                visitor.anonymousKeyHash(),
                content,
                LocalDateTime.now(ZoneOffset.UTC).minusHours(24)
        );
        if (duplicateCount > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "请勿重复提交相同内容");
        }
    }

    private ReplyTarget resolveReplyTarget(Long articleId, String type, Long requestedParentId) {
        if (requestedParentId == null) return ReplyTarget.rootComment();

        CommentRecord parent = requireComment(requestedParentId);
        boolean validParent = type.equals(parent.type())
                && Objects.equals(articleId, parent.articleId())
                && "APPROVED".equals(parent.status());
        if (!validParent) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "回复目标无效");
        }
        return ReplyTarget.replyTo(parent);
    }

    private void persistPendingComment(
            Long articleId,
            String type,
            CommentRequest request,
            Visitor visitor,
            String content,
            ReplyTarget replyTarget
    ) {
        var rendered = markdown.render(content);
        String email = policy.trimToNull(request.email());

        comments.insert(
                articleId,
                replyTarget.rootId(),
                replyTarget.parentId(),
                type,
                content,
                rendered.html(),
                request.nickname().trim(),
                cipher.encrypt(email),
                policy.normalizeWebsite(request.website()),
                visitor.anonymousKeyHash(),
                PENDING_STATUS,
                false,
                request.notifyOnReply() && email != null,
                visitor.ipHash(),
                visitor.userAgentSummary()
        );
    }

    private CommentRecord requireComment(Long id) {
        return comments.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "评论不存在"));
    }

    private record ReplyTarget(Long rootId, Long parentId) {

        private static ReplyTarget rootComment() {
            return new ReplyTarget(null, null);
        }

        private static ReplyTarget replyTo(CommentRecord parent) {
            Long rootId = parent.rootId() == null ? parent.id() : parent.rootId();
            // Public discussions are deliberately flattened to two levels.
            return new ReplyTarget(rootId, rootId);
        }
    }
}
