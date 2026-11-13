package com.learnova.mapper;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.learnova.dto.progress.LessonProgressResponse;
import com.learnova.model.Lesson;
import com.learnova.model.LessonProgress;
import com.learnova.model.ProgressStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class LessonProgressMapperTest {

    private final LessonProgressMapper mapper =
        Mappers.getMapper(LessonProgressMapper.class);

    @Test
    void shouldMapLessonProgressToResponse() {
        Lesson lesson = new Lesson();
        lesson.setId(10L);
        lesson.setTitle("Java Fundamentals");

        LocalDateTime completedAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();

        LessonProgress progress = new LessonProgress();
        progress.setLesson(lesson);
        progress.setStatus(ProgressStatus.COMPLETED);
        progress.setCompletedAt(completedAt);
        progress.setUpdatedAt(updatedAt);

        LessonProgressResponse response = mapper.toResponse(progress);

        assertEquals(10L, response.lessonId());
        assertEquals("Java Fundamentals", response.lessonTitle());
        assertEquals(ProgressStatus.COMPLETED, response.status());
        assertEquals(completedAt, response.completedAt());
        assertEquals(updatedAt, response.updatedAt());
    }

    @Test
    void shouldMapNullCompletedAt() {
        Lesson lesson = new Lesson();
        lesson.setId(10L);
        lesson.setTitle("Java Fundamentals");

        LessonProgress progress = new LessonProgress();
        progress.setLesson(lesson);
        progress.setStatus(ProgressStatus.IN_PROGRESS);

        LessonProgressResponse response = mapper.toResponse(progress);

        assertNull(response.completedAt());
    }
}

