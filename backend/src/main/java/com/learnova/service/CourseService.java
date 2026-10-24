package com.learnova.service;

import com.learnova.dto.CourseRequest;
import com.learnova.dto.CourseResponse;
import com.learnova.exception.CourseNotFoundException;
import com.learnova.mapper.CourseMapper;
import com.learnova.repository.CourseRepository;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CourseMapper courseMapper;

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
