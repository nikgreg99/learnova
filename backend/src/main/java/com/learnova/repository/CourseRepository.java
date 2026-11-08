package com.learnova.repository;

import com.learnova.model.Course;
import com.learnova.model.CourseStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByStatus(CourseStatus status);

    List<Course> findByInstructorId(Long instructorId);
}
