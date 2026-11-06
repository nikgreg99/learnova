package com.learnova.messaging.message;

public record EmailMessage(
        String recipient,
        String subject,
        String body) {
}
