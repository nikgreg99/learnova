package com.learnova.messaging.producer;

import com.learnova.config.rabbitmq.RabbitMQConfig;
import com.learnova.messaging.message.EmailMessage;

import lombok.RequiredArgsConstructor;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publish(EmailMessage message) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.EMAIL_ROUTING_KEY, message);
    }
}
