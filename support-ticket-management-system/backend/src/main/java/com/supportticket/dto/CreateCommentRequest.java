package com.supportticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "Author is required and must not be blank.")
        @Size(max = 100, message = "Author must be at most 100 characters.")
        String author,

        @NotBlank(message = "Comment text is required and must not be blank.")
        @Size(max = 2000, message = "Comment text must be at most 2,000 characters.")
        String text) {
}
