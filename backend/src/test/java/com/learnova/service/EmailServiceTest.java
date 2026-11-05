package com.learnova.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, "noreply@learnova.local");
    }

    @Test
    void sendEmail_shouldBuildAndSendMailMessage() {
        emailService.sendEmail("student@learnova.local", "Welcome to Learnova", "Hello student!");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();
        assertEquals("student@learnova.local", Objects.requireNonNull(sentMessage.getTo())[0]);
        assertEquals("Welcome to Learnova", sentMessage.getSubject());
        assertEquals("Hello student!", sentMessage.getText());
        assertEquals("noreply@learnova.local", sentMessage.getFrom());
    }
}
