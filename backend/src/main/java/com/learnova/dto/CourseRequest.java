package com.learnova.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CourseRequest(
        @NotBlank String name,
        String description,
        @PositiveOrZero BigDecimal price
) {}

