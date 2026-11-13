package com.learnova.repository;

import com.learnova.model.LessonProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;


public interface LessonProgressRepository
    extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByUser_IdAndLesson_Id(
        Long userId,
        Long lessonId
    );


    List<LessonProgress>
    findAllByUser_IdAndLesson_Course_IdOrderByLesson_PositionAsc(
        Long userId,
        Long courseId
    );
}
