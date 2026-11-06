package com.learnova.messaging.integration;

import com.learnova.config.rabbitmq.RabbitMQConfig;
import com.learnova.messaging.message.EmailMessage;
import com.learnova.messaging.producer.EmailMessageProducer;
import com.learnova.messaging.consumer.EmailMessageConsumer;
import com.learnova.service.EmailService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.reset;

@SpringBootTest(classes = EmailMessagingIntegrationTest.TestApplication.class)
@Testcontainers
class EmailMessagingIntegrationTest {

    static final RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:4-management").withAdminUser("learnova")
            .withAdminPassword("learnova");

    @DynamicPropertySource
    static void configureRabbitMQ(DynamicPropertyRegistry registry) {

        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "learnova");
        registry.add("spring.rabbitmq.password", () -> "learnova");
        registry.add("spring.rabbitmq.virtual-host", () -> "/");
    }

    @Autowired
    private EmailMessageProducer emailMessageProducer;

    @MockitoBean
    private EmailService emailService;

    private EmailMessage emailMessage;

    @BeforeEach
    void setUp() {

        reset(emailService);

        emailMessage = new EmailMessage("test@example.com", "Welcome to Learnova", "Welcome!");
    }

    @Test
    void publish_ShouldDeliverMessageToConsumer() {

        emailMessageProducer.publish(emailMessage);

        verify(emailService, timeout(5000)).sendEmail(emailMessage.recipient(), emailMessage.subject(),
                emailMessage.body());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableRabbit
    @Import({
            RabbitMQConfig.class,
            EmailMessageProducer.class,
            EmailMessageConsumer.class
    })
    static class TestApplication {
    }
}
