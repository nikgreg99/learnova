package com.learnova.messaging.consumer;

import com.learnova.config.rabbitmq.RabbitMQConfig;
import com.learnova.messaging.message.EmailMessage;
import com.learnova.service.EmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EmailMessageConsumer {

    private final EmailService emailService;

    public EmailMessageConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void consume(EmailMessage message) {

        emailService.sendEmail(message.recipient(), message.subject(), message.body());
    }
}
