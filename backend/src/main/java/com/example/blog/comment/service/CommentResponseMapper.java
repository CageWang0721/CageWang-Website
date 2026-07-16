package com.example.blog.comment.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.blog.comment.dto.AdminCommentItem;
import com.example.blog.comment.dto.PublicCommentResponse;
import com.example.blog.comment.model.AdminCommentRecord;
import com.example.blog.comment.model.CommentRecord;
import com.example.blog.notification.service.SensitiveDataCipher;

@Component
final class CommentResponseMapper {

    private final SensitiveDataCipher cipher;

    CommentResponseMapper(SensitiveDataCipher cipher) {
        this.cipher = cipher;
    }

    List<PublicCommentResponse> toPublicTree(List<CommentRecord> records) {
        Map<Long, List<CommentRecord>> repliesByRootId = new LinkedHashMap<>();
        List<CommentRecord> roots = new ArrayList<>();

        for (CommentRecord comment : records) {
            if (isRoot(comment)) {
                roots.add(comment);
                continue;
            }

            Long rootId = comment.rootId() != null ? comment.rootId() : comment.parentId();
            repliesByRootId.computeIfAbsent(rootId, ignored -> new ArrayList<>()).add(comment);
        }

        return roots.stream()
                .map(root -> toPublicComment(
                        root,
                        repliesByRootId.getOrDefault(root.id(), List.of()).stream()
                                .map(reply -> toPublicComment(reply, List.of()))
                                .toList()
                ))
                .toList();
    }

    PublicCommentResponse toPublicComment(CommentRecord comment) {
        return toPublicComment(comment, List.of());
    }

    AdminCommentItem toAdminItem(AdminCommentRecord comment) {
        return new AdminCommentItem(
                comment.id(),
                comment.articleId(),
                comment.articleTitle(),
                comment.parentId(),
                comment.type(),
                comment.contentMarkdown(),
                comment.nickname(),
                maskEmail(comment.emailCiphertext()),
                comment.website(),
                comment.status(),
                comment.adminReply(),
                comment.ipSummary(),
                comment.createdAt()
        );
    }

    private boolean isRoot(CommentRecord comment) {
        return comment.rootId() == null && comment.parentId() == null;
    }

    private PublicCommentResponse toPublicComment(
            CommentRecord comment,
            List<PublicCommentResponse> replies
    ) {
        return new PublicCommentResponse(
                comment.id(),
                comment.parentId(),
                comment.nickname(),
                comment.contentHtml(),
                comment.adminReply(),
                comment.createdAt(),
                replies
        );
    }

    private String maskEmail(byte[] encryptedEmail) {
        if (encryptedEmail == null) return null;
        try {
            String email = cipher.decrypt(encryptedEmail);
            int at = email == null ? -1 : email.indexOf('@');
            if (at <= 0 || at == email.length() - 1) return "***";
            return email.substring(0, 1) + "***" + email.substring(at);
        } catch (RuntimeException exception) {
            return "已加密";
        }
    }
}
