package com.supportticket.dto;

import com.supportticket.model.Comment;
import java.time.Instant;

public record CommentResponse(Long id, String author, String text, Instant createdAt) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getAuthor(), comment.getText(), comment.getCreatedAt());
    }
}
