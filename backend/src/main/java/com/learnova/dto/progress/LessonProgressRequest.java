package com.learnova.dto.progress;

import com.learnova.model.ProgressStatus;
import jakarta.validation.constraints.NotNull;

public record LessonProgressRequest(
    @NotNull ProgressStatus status
) {
}
