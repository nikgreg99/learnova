package com.learnova.mapper;

import com.learnova.dto.CourseRequest;
import com.learnova.dto.CourseResponse;
import com.learnova.model.Course;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    Course toEntity(CourseRequest courseRequest);

    CourseResponse toResponse(Course courseResponse);

    void updateCourse(
            CourseRequest request,
            @MappingTarget Course course
    );

}
