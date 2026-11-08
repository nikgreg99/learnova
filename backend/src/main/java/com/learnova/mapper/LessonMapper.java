package com.learnova.mapper;

import com.learnova.dto.lesson.LessonPatchRequest;
import com.learnova.dto.lesson.LessonRequest;
import com.learnova.dto.lesson.LessonResponse;
import com.learnova.model.Lesson;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LessonMapper {

    Lesson toEntity(LessonRequest request);

    @Mapping(source = "course.id", target = "courseId")
    LessonResponse toResponse(Lesson lesson);

    void updateLesson(LessonRequest request, @MappingTarget Lesson lesson);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void patchLesson(LessonPatchRequest request, @MappingTarget Lesson lesson);
}
