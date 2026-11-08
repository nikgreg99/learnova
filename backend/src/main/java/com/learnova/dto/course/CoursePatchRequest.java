package com.learnova.dto.course;

import com.learnova.model.CourseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@Schema(description = "Request payload for partially updating a course")
public record CoursePatchRequest(
        @Schema(description = "Name of the course", example = "Advanced Spring Boot") String name,
        @Schema(description = "Description of the course", example = "Deep dive into Spring Boot internals") String description,
        @Schema(description = "Price of the course", example = "59.99") @PositiveOrZero BigDecimal price,
        @Schema(description = "Publication status of the course", example = "PUBLISHED") CourseStatus status) {
}
