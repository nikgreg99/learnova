package com.learnova.mapper;

import com.learnova.dto.progress.CourseProgressResponse;
import com.learnova.dto.progress.LessonProgressResponse;
import com.learnova.model.Course;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CourseProgressMapper {

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseName", source = "course.name")
    @Mapping(target = "totalLessons", source = "totalLessons")
    @Mapping(target = "completedLessons", source = "completed")
    @Mapping(target = "inProgressLessons", source = "inProgress")
    @Mapping(target = "notStartedLessons", source = "notStarted")
    @Mapping(target = "completionPercentage", source = "completionPercentage")
    @Mapping(target = "lessons", source = "lessonResponses")
    CourseProgressResponse toResponse(
        Course course,
        int totalLessons,
        int completed,
        int inProgress,
        int notStarted,
        double completionPercentage,
        List<LessonProgressResponse> lessonResponses
    );
}
