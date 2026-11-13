package com.learnova.mapper;

import com.learnova.dto.progress.LessonProgressResponse;
import com.learnova.model.LessonProgress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LessonProgressMapper {

    @Mapping(target = "lessonId", source = "lesson.id")
    @Mapping(target = "lessonTitle", source = "lesson.title")
    LessonProgressResponse toResponse(LessonProgress progress);
}
