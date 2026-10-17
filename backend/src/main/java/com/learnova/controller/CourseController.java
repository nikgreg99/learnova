package com.learnova.controller;

import com.learnova.dto.CourseRequest;
import com.learnova.dto.CourseResponse;
import com.learnova.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CourseController {

    @Autowired
    private CourseService courseService;

    @GetMapping("/courses")
    public ResponseEntity<List<CourseResponse>> getCourses(){
        return ResponseEntity.ok(courseService.getCourses());
    }

    @GetMapping("/courses/{id}")
    public ResponseEntity<?> getCourse(@PathVariable Long id){
        var course = courseService.getCourseById(id);
        return course.isPresent() ? ResponseEntity.ok(course.get()) : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @PutMapping("/courses/{id}")
    public ResponseEntity<?> updateCourse
            (@PathVariable Long id,
             @RequestBody CourseRequest courseRequest){
        var course = courseService.updateCourse(id, courseRequest);
        return  course.isPresent() ? ResponseEntity.ok(course.get()) : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @PostMapping("/courses")
    public ResponseEntity<CourseResponse> addCourse(@RequestBody CourseRequest course){
      var createdCourse = courseService.addCourse(course);
      return ResponseEntity.status(HttpStatus.CREATED).body(createdCourse);
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id){
        courseService.deleteCourseById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
