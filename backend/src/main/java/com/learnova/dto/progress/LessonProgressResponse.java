package com.learnova.dto.progress;

import com.learnova.model.ProgressStatus;

import java.time.LocalDateTime;

public record LessonProgressResponse(
    Long lessonId,
    String lessonTitle,
    ProgressStatus status,
    LocalDateTime completedAt,
    LocalDateTime updatedAt
) {}
