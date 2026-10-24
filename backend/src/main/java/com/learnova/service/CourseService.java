package com.learnova.service;

import com.learnova.dto.CourseRequest;
import com.learnova.dto.CourseResponse;
import com.learnova.exception.CourseNotFoundException;
import com.learnova.mapper.CourseMapper;
import com.learnova.repoository.CourseRepository;
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

    public CourseResponse getCourseById(Long id) {
        return courseRepository
                .findById(id)
                .map(courseMapper::toResponse)
                .orElseThrow(()-> new CourseNotFoundException(id));
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

    public boolean deleteCourseById(Long id) {

        if(!courseRepository.existsById(id)){
            return false;
        }

        courseRepository.deleteById(id);
        return true;
    }
}
