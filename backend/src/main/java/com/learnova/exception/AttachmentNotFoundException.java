package com.learnova.exception;

import lombok.Getter;

@Getter
public class AttachmentNotFoundException extends RuntimeException {

    private final Long attachmentId;


    public AttachmentNotFoundException(Long attachmentId) {
        this.attachmentId = attachmentId;
    }
}
