package com.learnova.dto.progress;

import java.util.List;

public record StudentProgressResponse(
    List<CourseProgressResponse> courses
) {}
