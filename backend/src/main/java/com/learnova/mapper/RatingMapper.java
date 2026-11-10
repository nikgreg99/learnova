package com.learnova.mapper;

import com.learnova.dto.rating.RatingRequest;
import com.learnova.dto.rating.RatingResponse;
import com.learnova.model.Rating;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RatingMapper {

    Rating toEntity(RatingRequest request);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "course.id", target = "courseId")
    @Mapping(source = "lesson.id", target = "lessonId")
    RatingResponse toResponse(Rating rating);

    void updateRating(RatingRequest request, @MappingTarget Rating rating);
}
