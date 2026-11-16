package com.learnova.repository;

import com.learnova.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findAllByCourse_IdOrderByCreatedAtDesc(Long courseId);

    List<Attachment> findAllByLesson_IdOrderByCreatedAtDesc(Long lessonId);

}
