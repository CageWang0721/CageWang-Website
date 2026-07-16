package com.example.blog.comment.service;

import org.springframework.stereotype.Component;

import com.example.blog.comment.mapper.CommentMapper;
import com.example.blog.comment.model.CommentRecord;
import com.example.blog.notification.config.MailNotificationProperties;
import com.example.blog.notification.service.NotificationService;

@Component
final class CommentReplyNotifier {

    private static final int MAX_REPLY_PREVIEW_LENGTH = 160;

    private final CommentMapper comments;
    private final NotificationService notifications;
    private final MailNotificationProperties mailProperties;

    CommentReplyNotifier(
            CommentMapper comments,
            NotificationService notifications,
            MailNotificationProperties mailProperties
    ) {
        this.comments = comments;
        this.notifications = notifications;
        this.mailProperties = mailProperties;
    }

    void notifyParentOfApprovedReply(CommentRecord reply) {
        if (reply.parentId() == null) return;

        comments.findById(reply.parentId())
                .filter(CommentRecord::notifyOnReply)
                .ifPresent(parent -> notifyReply(parent, reply.contentMarkdown()));
    }

    void notifyReply(CommentRecord target, String replyContent) {
        if (!target.notifyOnReply() || target.emailCiphertext() == null) return;

        notifications.enqueueReply(
                target.emailCiphertext(),
                target.nickname(),
                replyPreview(replyContent),
                targetUrl(target)
        );
    }

    private String replyPreview(String content) {
        if (content.length() <= MAX_REPLY_PREVIEW_LENGTH) return content;
        return content.substring(0, MAX_REPLY_PREVIEW_LENGTH) + "…";
    }

    private String targetUrl(CommentRecord target) {
        String path = target.articleId() == null
                ? "/message#message-" + target.id()
                : "/article/" + comments.findArticleSlug(target.articleId()).orElse("")
                    + "#comment-" + target.id();
        String siteUrl = mailProperties.siteUrl() == null
                ? "http://localhost"
                : mailProperties.siteUrl();
        return siteUrl.replaceAll("/+$", "") + path;
    }
}
