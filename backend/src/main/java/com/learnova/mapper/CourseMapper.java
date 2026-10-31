package com.learnova.mapper;

import com.learnova.dto.course.CourseRequest;
import com.learnova.dto.course.CourseResponse;
import com.learnova.model.Course;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CourseMapper {

    Course toEntity(CourseRequest courseRequest);

    CourseResponse toResponse(Course courseResponse);

    void updateCourse(
            CourseRequest request,
            @MappingTarget Course course
    );

}
