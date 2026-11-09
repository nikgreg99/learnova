package com.learnova.service;

import com.learnova.dto.course.CoursePatchRequest;
import com.learnova.dto.course.CourseRequest;
import com.learnova.dto.course.CourseResponse;
import com.learnova.exception.CourseNotFoundException;
import com.learnova.mapper.CourseMapper;
import com.learnova.model.CourseStatus;
import com.learnova.repository.CourseRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    public CourseService(CourseRepository courseRepository, CourseMapper courseMapper) {
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
    }

    public List<CourseResponse> getCourses() {
        return courseRepository.findAll().stream().map(courseMapper::toResponse).toList();
    }

    public Optional<CourseResponse> getCourseById(Long id) {
        return courseRepository.findById(id).map(courseMapper::toResponse);
    }

    public CourseResponse addCourse(CourseRequest courseRequest) {
        var course = courseMapper.toEntity(courseRequest);
        var savedCourse = courseRepository.save(course);
        return courseMapper.toResponse(savedCourse);
    }

    public Optional<CourseResponse> updateCourse(Long courseId, CourseRequest courseRequest) {
        return courseRepository.findById(courseId).map(course -> {
            courseMapper.updateCourse(courseRequest, course);
            var updatedCourse = courseRepository.save(course);
            return courseMapper.toResponse(updatedCourse);
        });
    }

    public Optional<CourseResponse> patchCourse(Long courseId, CoursePatchRequest request) {
        return courseRepository.findById(courseId).map(course -> {
            courseMapper.patchCourse(request, course);
            var updatedCourse = courseRepository.save(course);
            return courseMapper.toResponse(updatedCourse);
        });
    }

    public Optional<CourseResponse> updateCourseStatus(Long courseId, CourseStatus status) {
        return courseRepository.findById(courseId).map(course -> {
            course.setStatus(status);
            var updatedCourse = courseRepository.save(course);
            return courseMapper.toResponse(updatedCourse);
        });
    }

    public void deleteCourseById(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new CourseNotFoundException(id);
        }
        courseRepository.deleteById(id);
    }
}
