package com.learnova.mapper;

import com.learnova.dto.enrollment.EnrollmentResponse;
import com.learnova.model.Enrollment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {

    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentUsername", source = "student.username")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseName", source = "course.name")
    EnrollmentResponse toResponse(Enrollment enrollment);
}
