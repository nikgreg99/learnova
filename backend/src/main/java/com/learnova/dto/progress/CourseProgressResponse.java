package com.learnova.dto.progress;

import java.util.List;

public record CourseProgressResponse(
    Long courseId,
    String courseName,
    int totalLessons,
    int completedLessons,
    int inProgressLessons,
    int notStartedLessons,
    double completionPercentage,
    List<LessonProgressResponse> lessons
) {}
