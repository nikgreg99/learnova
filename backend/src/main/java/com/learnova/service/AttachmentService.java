package com.learnova.service;

import com.learnova.dto.attachment.AttachmentDownload;
import com.learnova.exception.AttachmentNotFoundException;
import com.learnova.exception.CourseNotFoundException;
import com.learnova.exception.LessonNotFoundException;
import com.learnova.handler.AttachmentStorageTransactionHandler;
import com.learnova.service.security.AttachmentSecurityService;
import com.learnova.storage.FileStorageService;
import com.learnova.validator.AttachmentValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.learnova.config.security.SecurityUtils;
import com.learnova.dto.attachment.AttachmentResponse;
import com.learnova.mapper.AttachmentMapper;
import com.learnova.model.Attachment;
import com.learnova.model.Course;
import com.learnova.model.Lesson;
import com.learnova.repository.AttachmentRepository;
import com.learnova.repository.CourseRepository;
import com.learnova.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final AttachmentMapper attachmentMapper;
    private final FileStorageService fileStorageService;
    private final AttachmentValidator attachmentValidator;
    private final AttachmentSecurityService attachmentSecurityService;
    private final AttachmentStorageTransactionHandler attachmentStorageTransactionHandler;

    @Transactional
    public AttachmentResponse upload(
        MultipartFile file,
        Long courseId,
        Long lessonId
    ) {
        attachmentValidator.validateUpload(file, courseId, lessonId);

        Course course = null;
        Lesson lesson = null;

        if (courseId != null) {
            course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                    new CourseNotFoundException(courseId)
                );
        }

        if (lessonId != null) {
            lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() ->
                    new LessonNotFoundException(lessonId)
                );
        }

        String storageKey = UUID.randomUUID().toString();
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType() != null
            ? file.getContentType()
            : "application/octet-stream";

        try (InputStream inputStream = file.getInputStream()) {
            fileStorageService.upload(
                storageKey,
                inputStream,
                file.getSize(),
                contentType
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read uploaded file: " + originalFilename,
                exception
            );
        }

        attachmentStorageTransactionHandler.registerUploadRollback(storageKey);

        Attachment attachment = new Attachment();
        attachment.setOriginalFilename(originalFilename);
        attachment.setStorageKey(storageKey);
        attachment.setContentType(contentType);
        attachment.setSize(file.getSize());
        attachment.setUploadedBy(SecurityUtils.getCurrentKeycloakId());
        attachment.setCourse(course);
        attachment.setLesson(lesson);

        Attachment savedAttachment = attachmentRepository.save(attachment);

        return attachmentMapper.toResponse(savedAttachment);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getCourseAttachments(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }

        return attachmentRepository
            .findAllByCourse_IdOrderByCreatedAtDesc(courseId)
            .stream()
            .map(attachmentMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getLessonAttachments(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new LessonNotFoundException(lessonId);
        }

        return attachmentRepository
            .findAllByLesson_IdOrderByCreatedAtDesc(lessonId)
            .stream()
            .map(attachmentMapper::toResponse)
            .toList();
    }

    @Transactional
    public void delete(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
            .orElseThrow(() ->
                new AttachmentNotFoundException(attachmentId)
            );

        attachmentSecurityService.validateOwnership(attachment);

        String storageKey = attachment.getStorageKey();

        attachmentRepository.delete(attachment);

        attachmentStorageTransactionHandler.registerDeleteAfterCommit(storageKey);
    }

    @Transactional(readOnly = true)
    public AttachmentDownload download(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
            .orElseThrow(() ->
                new AttachmentNotFoundException(attachmentId));

        InputStream inputStream = fileStorageService.download(
            attachment.getStorageKey()
        );

        return new AttachmentDownload(
            inputStream,
            attachment.getOriginalFilename(),
            attachment.getContentType(),
            attachment.getSize()
        );
    }

}

