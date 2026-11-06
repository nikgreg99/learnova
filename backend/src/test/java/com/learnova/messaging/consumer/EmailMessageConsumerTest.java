package com.learnova.messaging.consumer;

import com.learnova.messaging.message.EmailMessage;
import com.learnova.service.EmailService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailMessageConsumerTest {

    @Mock
    private EmailService emailService;

    private EmailMessageConsumer emailMessageConsumer;

    private EmailMessage emailMessage;

    @BeforeEach
    void setUp() {
        emailMessageConsumer = new EmailMessageConsumer(emailService);

        emailMessage = new EmailMessage("test@example.com", "Welcome to Learnova", "Welcome!");
    }

    @Test
    void consume_ShouldSendEmail() {

        emailMessageConsumer.consume(emailMessage);

        verify(emailService).sendEmail(emailMessage.recipient(), emailMessage.subject(), emailMessage.body());
    }

    @Test
    void consume_WhenEmailServiceFails_ShouldPropagateException() {

        doThrow(new RuntimeException("SMTP error")).when(emailService).sendEmail(emailMessage.recipient(),
                emailMessage.subject(), emailMessage.body());

        assertThrows(RuntimeException.class, () -> emailMessageConsumer.consume(emailMessage));
    }
}
