package com.learnova.controller;


import com.learnova.dto.progress.CourseProgressResponse;
import com.learnova.dto.progress.LessonProgressRequest;
import com.learnova.dto.progress.LessonProgressResponse;
import com.learnova.dto.progress.StudentProgressResponse;
import com.learnova.service.LessonProgressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('STUDENT')")
public class LessonProgressController {

    private final LessonProgressService lessonProgressService;

    public LessonProgressController(LessonProgressService lessonProgressService) {
        this.lessonProgressService = lessonProgressService;
    }

    @PostMapping("/lessons/{lessonId}/progress")
    public ResponseEntity<LessonProgressResponse> updateLessonProgress(
        @PathVariable Long lessonId,
        @Valid @RequestBody LessonProgressRequest request
    ) {
        return ResponseEntity.ok(
            lessonProgressService.updateLessonProgress(
                lessonId,
                request.status()
            )
        );
    }

    @GetMapping("/courses/{courseId}/progress")
    public ResponseEntity<CourseProgressResponse> getCourseProgress(
        @PathVariable Long courseId
    ){
        return ResponseEntity.ok(
            lessonProgressService.getCourseProgress(courseId)
        );
    }

    @GetMapping("/users/me/progress")
    public ResponseEntity<StudentProgressResponse> getStudentProgress() {
        return ResponseEntity.ok(
            lessonProgressService.getStudentProgress()
        );
    }
}

