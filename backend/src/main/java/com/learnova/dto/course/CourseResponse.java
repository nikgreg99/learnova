package com.learnova.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CourseResponse(

        Long id,
        @NotBlank
        String name,
        String description,
        @PositiveOrZero
        @NotNull
        BigDecimal price,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
