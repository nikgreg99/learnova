package com.learnova.service;

import com.learnova.dto.CourseRequest;
import com.learnova.dto.CourseResponse;
import com.learnova.exception.CourseNotFoundException;
import com.learnova.mapper.CourseMapper;
import com.learnova.repository.CourseRepository;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    public CourseService(CourseRepository courseRepository, CourseMapper courseMapper) {
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
    }

    public List<CourseResponse> getCourses(){
        return courseRepository.findAll()
                .stream()
                .map(courseMapper::toResponse)
                .toList();
    }

    public Optional<CourseResponse> getCourseById(Long id) {
        return courseRepository
                .findById(id)
                .map(courseMapper::toResponse);
    }

    public CourseResponse addCourse(CourseRequest courseRequest) {
        var course = courseMapper.toEntity(courseRequest);
        var savedCourse = courseRepository.save(course);
        return courseMapper.toResponse(savedCourse);
    }

    public Optional<CourseResponse> updateCourse(Long courseId,CourseRequest courseRequest) {
        return courseRepository
                .findById(courseId)
                .map(course ->  {
                    courseMapper.updateCourse(courseRequest,course);
                    var updatedCourse = courseRepository.save(course);
                    return courseMapper.toResponse(updatedCourse);
                });
    }

    @Transactional
    public void deleteCourseById(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new CourseNotFoundException(id);
        }
        courseRepository.deleteById(id);
    }


}
