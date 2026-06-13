package com.example.springaichat.model;

import java.time.LocalDateTime;

public record ClassInfo(
        Long id,
        String className,
        String homeroomTeacher,
        Integer studentCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
