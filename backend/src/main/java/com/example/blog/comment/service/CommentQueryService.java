package com.example.blog.comment.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.blog.comment.dto.AdminCommentPage;
import com.example.blog.comment.dto.PublicCommentResponse;
import com.example.blog.comment.mapper.CommentMapper;

@Service
public class CommentQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CommentMapper comments;
    private final CommentPolicy policy;
    private final CommentResponseMapper responses;

    public CommentQueryService(
            CommentMapper comments,
            CommentPolicy policy,
            CommentResponseMapper responses
    ) {
        this.comments = comments;
        this.policy = policy;
        this.responses = responses;
    }

    public List<PublicCommentResponse> articleComments(Long articleId) {
        return responses.toPublicTree(comments.findApprovedArticleComments(articleId));
    }

    public List<PublicCommentResponse> messages() {
        return responses.toPublicTree(comments.findApprovedMessages());
    }

    public AdminCommentPage adminPage(
            String status,
            String type,
            String keyword,
            int page,
            int pageSize
    ) {
        String statusFilter = policy.normalizeStatusFilter(status);
        String typeFilter = policy.normalizeTypeFilter(type);
        String keywordFilter = policy.trimToNull(keyword);
        int currentPage = Math.max(1, page);
        int currentPageSize = Math.clamp(pageSize, 1, MAX_PAGE_SIZE);
        long offset = (long) (currentPage - 1) * currentPageSize;

        return new AdminCommentPage(
                comments.findAdminPage(
                        statusFilter,
                        typeFilter,
                        keywordFilter,
                        offset,
                        currentPageSize
                ).stream().map(responses::toAdminItem).toList(),
                comments.countAdminPage(statusFilter, typeFilter, keywordFilter),
                currentPage,
                currentPageSize
        );
    }
}
