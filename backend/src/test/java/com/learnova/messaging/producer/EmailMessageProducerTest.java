package com.learnova.messaging.producer;

import com.learnova.config.rabbitmq.RabbitMQConfig;
import com.learnova.messaging.message.EmailMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailMessageProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private EmailMessageProducer emailMessageProducer;

    private EmailMessage emailMessage;

    @BeforeEach
    void setUp() {
        emailMessageProducer = new EmailMessageProducer(rabbitTemplate);

        emailMessage = new EmailMessage("test@example.com", "Welcome to Learnova", "Welcome!");
    }

    @Test
    void publish_ShouldSendMessageToRabbitMQ() {

        emailMessageProducer.publish(emailMessage);

        verify(rabbitTemplate).convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.EMAIL_ROUTING_KEY, emailMessage);
    }
}
