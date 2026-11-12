package com.learnova.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnova.dto.lesson.LessonPatchRequest;
import com.learnova.dto.lesson.LessonRequest;
import com.learnova.dto.lesson.LessonResponse;
import com.learnova.model.Course;
import com.learnova.model.Lesson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class LessonMapperTest {

    private final LessonMapper lessonMapper =
        Mappers.getMapper(LessonMapper.class);

    private Course course;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(1L);
    }

    @Test
    void toEntity_shouldMapLessonRequest() {
        LessonRequest request = new LessonRequest(
            "Introduction to Java",
            "Java basics",
            "Lesson content",
            1,
            3600
        );

        Lesson result = lessonMapper.toEntity(request);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Introduction to Java");
        assertThat(result.getDescription()).isEqualTo("Java basics");
        assertThat(result.getContent()).isEqualTo("Lesson content");
        assertThat(result.getPosition()).isEqualTo(1);
        assertThat(result.getDuration()).isEqualTo(3600);
    }

    @Test
    void toResponse_shouldMapLessonAndCourseId() {
        Lesson lesson = new Lesson();
        lesson.setId(10L);
        lesson.setTitle("Introduction to Java");
        lesson.setDescription("Java basics");
        lesson.setContent("Lesson content");
        lesson.setPosition(1);
        lesson.setDuration(3600);
        lesson.setCourse(course);

        LessonResponse result = lessonMapper.toResponse(lesson);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.title()).isEqualTo("Introduction to Java");
        assertThat(result.description()).isEqualTo("Java basics");
        assertThat(result.content()).isEqualTo("Lesson content");
        assertThat(result.position()).isEqualTo(1);
        assertThat(result.duration()).isEqualTo(3600);
        assertThat(result.courseId()).isEqualTo(1L);
    }

    @Test
    void updateLesson_shouldUpdateExistingLesson() {
        LessonRequest request = new LessonRequest(
            "Advanced Java",
            "Advanced concepts",
            "Updated content",
            2,
            7200
        );

        Lesson lesson = new Lesson();
        lesson.setId(10L);
        lesson.setTitle("Introduction to Java");
        lesson.setCourse(course);

        lessonMapper.updateLesson(request, lesson);

        assertThat(lesson.getId()).isEqualTo(10L);
        assertThat(lesson.getTitle()).isEqualTo("Advanced Java");
        assertThat(lesson.getDescription()).isEqualTo("Advanced concepts");
        assertThat(lesson.getContent()).isEqualTo("Updated content");
        assertThat(lesson.getPosition()).isEqualTo(2);
        assertThat(lesson.getDuration()).isEqualTo(7200);

        // Relationships and identifiers must remain unchanged.
        assertThat(lesson.getCourse()).isSameAs(course);
    }

    @Test
    void patchLesson_shouldUpdateOnlyNonNullFields() {
        LessonPatchRequest request = new LessonPatchRequest(
            "Updated title",
            null,
            "Updated content",
            null,
            null
        );

        Lesson lesson = new Lesson();
        lesson.setId(10L);
        lesson.setTitle("Original title");
        lesson.setDescription("Original description");
        lesson.setContent("Original content");
        lesson.setPosition(1);
        lesson.setDuration(3600);
        lesson.setCourse(course);

        lessonMapper.patchLesson(request, lesson);

        assertThat(lesson.getTitle()).isEqualTo("Updated title");
        assertThat(lesson.getDescription()).isEqualTo("Original description");
        assertThat(lesson.getContent()).isEqualTo("Updated content");
        assertThat(lesson.getPosition()).isEqualTo(1);
        assertThat(lesson.getDuration()).isEqualTo(3600);

        assertThat(lesson.getId()).isEqualTo(10L);
        assertThat(lesson.getCourse()).isSameAs(course);
    }

    @Test
    void patchLesson_shouldNotChangeAnythingWhenAllFieldsAreNull() {
        LessonPatchRequest request = new LessonPatchRequest(
            null,
            null,
            null,
            null,
            null
        );

        Lesson lesson = new Lesson();
        lesson.setId(10L);
        lesson.setTitle("Original title");
        lesson.setDescription("Original description");
        lesson.setContent("Original content");
        lesson.setPosition(1);
        lesson.setDuration(3600);
        lesson.setCourse(course);

        lessonMapper.patchLesson(request, lesson);

        assertThat(lesson.getId()).isEqualTo(10L);
        assertThat(lesson.getTitle()).isEqualTo("Original title");
        assertThat(lesson.getDescription()).isEqualTo("Original description");
        assertThat(lesson.getContent()).isEqualTo("Original content");
        assertThat(lesson.getPosition()).isEqualTo(1);
        assertThat(lesson.getDuration()).isEqualTo(3600);
        assertThat(lesson.getCourse()).isSameAs(course);
    }
}
