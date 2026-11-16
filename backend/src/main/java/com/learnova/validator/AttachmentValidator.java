package com.learnova.validator;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AttachmentValidator {

    public void validateUpload(
        MultipartFile file,
        Long courseId,
        Long lessonId
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        if ((courseId == null) == (lessonId == null)) {
            throw new IllegalArgumentException(
                "An attachment must belong to exactly one course or lesson"
            );
        }

        if (file.getOriginalFilename() == null
            || file.getOriginalFilename().isBlank()) {
            throw new IllegalArgumentException(
                "File name must not be empty"
            );
        }
    }

}
