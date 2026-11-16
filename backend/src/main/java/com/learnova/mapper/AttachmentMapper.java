package com.learnova.mapper;


import com.learnova.dto.attachment.AttachmentResponse;
import com.learnova.model.Attachment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttachmentMapper {

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "lessonId", source = "lesson.id")
    AttachmentResponse toResponse(Attachment attachment);
}

