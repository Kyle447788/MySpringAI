package com.example.springaichat.model;

import java.time.LocalDateTime;

public record ChatMessageRecord(
        Long id,
        String conversationId,
        String role,
        String content,
        Integer sequenceNo,
        LocalDateTime createdAt
) {
}
