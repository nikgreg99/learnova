package com.learnova.controller;

import com.learnova.dto.CourseRequest;
import com.learnova.dto.CourseResponse;
import com.learnova.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/courses")
    public ResponseEntity<List<CourseResponse>> getCourses(){
        return ResponseEntity.ok(courseService.getCourses());
    }

    @GetMapping("/courses/{id}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable Long id){
        return courseService.getCourseById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/courses/{id}")
    public ResponseEntity<CourseResponse> updateCourse
            (@PathVariable Long id,
             @RequestBody @Valid CourseRequest courseRequest){
        var course = courseService.updateCourse(id, courseRequest);
        return course
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/courses")
    public ResponseEntity<CourseResponse> addCourse(@RequestBody @Valid CourseRequest course){
      var createdCourse = courseService.addCourse(course);
      return ResponseEntity.status(HttpStatus.CREATED).body(createdCourse);
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id){
        courseService.deleteCourseById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
