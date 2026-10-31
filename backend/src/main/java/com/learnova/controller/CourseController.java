package com.learnova.controller;

import com.learnova.dto.course.CourseRequest;
import com.learnova.dto.course.CourseResponse;
import com.learnova.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getCourses(){
        return ResponseEntity.ok(courseService.getCourses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable Long id){
        return courseService.getCourseById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse
            (@PathVariable Long id,
             @RequestBody @Valid CourseRequest courseRequest){
        var course = courseService.updateCourse(id, courseRequest);
        return course
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CourseResponse> addCourse(@RequestBody @Valid CourseRequest course){
      var createdCourse = courseService.addCourse(course);
      return ResponseEntity.status(HttpStatus.CREATED).body(createdCourse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id){
        courseService.deleteCourseById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
