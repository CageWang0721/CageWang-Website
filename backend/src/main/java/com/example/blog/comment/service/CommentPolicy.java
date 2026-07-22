package com.example.blog.comment.service;

import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.example.blog.shared.error.ApiException;

@Component
final class CommentPolicy {

    private static final Set<String> STATUSES =
            Set.of("PENDING", "APPROVED", "REJECTED", "SPAM", "HIDDEN");
    private static final Set<String> TYPES = Set.of("ARTICLE", "MESSAGE");
    private static final Pattern LINK_PATTERN = Pattern.compile("(?i)(https?://|www\\.)");

    String normalizeStatusFilter(String value) {
        return normalizeFilter(value, STATUSES, "评论状态");
    }

    String normalizeTypeFilter(String value) {
        return normalizeFilter(value, TYPES, "互动类型");
    }

    void validateModerationStatus(String status) {
        if (!STATUSES.contains(status) || "PENDING".equals(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "无效的审核状态");
        }
    }

    String validateContent(String value) {
        String content = value == null ? "" : value.trim();
        if (content.length() < 2 || content.length() > 2000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "评论内容需要 2–2000 个字符");
        }
        if (LINK_PATTERN.matcher(content).results().count() > 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "评论中的链接不能超过 2 个");
        }
        return content;
    }

    String normalizeWebsite(String value) {
        String website = trimToNull(value);
        if (website == null) return null;
        if (!website.startsWith("http://") && !website.startsWith("https://")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "个人网站必须以 http:// 或 https:// 开头");
        }
        return website;
    }

    String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeFilter(String value, Set<String> allowed, String label) {
        String normalized = trimToNull(value);
        if (normalized == null) return null;
        normalized = normalized.toUpperCase();
        if (!allowed.contains(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, label + "无效");
        }
        return normalized;
    }
}
