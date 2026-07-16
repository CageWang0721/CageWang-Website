package com.example.blog.comment.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.blog.comment.dto.AdminCommentPage;
import com.example.blog.comment.dto.CommentRequest;
import com.example.blog.comment.dto.CommentSubmitResponse;
import com.example.blog.comment.dto.PublicCommentResponse;
import com.example.blog.interaction.service.VisitorContext.Visitor;

@Service
public class CommentService {

    private final CommentQueryService queries;
    private final CommentSubmissionService submissions;
    private final CommentModerationService moderation;

    public CommentService(
            CommentQueryService queries,
            CommentSubmissionService submissions,
            CommentModerationService moderation
    ) {
        this.queries = queries;
        this.submissions = submissions;
        this.moderation = moderation;
    }

    public List<PublicCommentResponse> articleComments(Long articleId) {
        return queries.articleComments(articleId);
    }

    public List<PublicCommentResponse> messages() {
        return queries.messages();
    }

    public AdminCommentPage adminPage(
            String status,
            String type,
            String keyword,
            int page,
            int pageSize
    ) {
        return queries.adminPage(status, type, keyword, page, pageSize);
    }

    public CommentSubmitResponse submitArticle(
            Long articleId,
            CommentRequest request,
            Visitor visitor
    ) {
        return submissions.submitArticle(articleId, request, visitor);
    }

    public CommentSubmitResponse submitMessage(CommentRequest request, Visitor visitor) {
        return submissions.submitMessage(request, visitor);
    }

    public void moderate(Long id, String status, Long operatorId) {
        moderation.moderate(id, status, operatorId);
    }

    public void delete(Long id, Long operatorId) {
        moderation.delete(id, operatorId);
    }

    public PublicCommentResponse adminReply(Long id, String content, Long operatorId) {
        return moderation.adminReply(id, content, operatorId);
    }
}
