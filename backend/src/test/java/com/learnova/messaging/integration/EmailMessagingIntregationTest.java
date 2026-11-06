package com.learnova.messaging.integration;

import com.learnova.config.rabbitmq.RabbitMQConfig;
import com.learnova.messaging.message.EmailMessage;
import com.learnova.messaging.consumer.EmailMessageConsumer;
import com.learnova.messaging.producer.EmailMessageProducer;
import com.learnova.service.EmailService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.reset;

@SpringBootTest(classes = EmailMessagingIntegrationTest.TestApplication.class)
class EmailMessagingIntegrationTest {

    static final RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:4-management").withAdminUser("learnova")
            .withAdminPassword("learnova");

    @BeforeAll
    static void startRabbitMQ() {
        rabbitMQ.start();
    }

    @AfterAll
    static void stopRabbitMQ() {
        rabbitMQ.stop();
    }

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

    @Autowired
    private RabbitTemplate rabbitTemplate;

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

    @Test
    void publish_WhenEmailServiceFailsOnce_ShouldRetryAndDeliverMessage() {

        doThrow(new RuntimeException("Temporary SMTP error")).doNothing().when(emailService)
                .sendEmail(emailMessage.recipient(), emailMessage.subject(), emailMessage.body());

        emailMessageProducer.publish(emailMessage);

        verify(emailService, timeout(10000).times(2)).sendEmail(emailMessage.recipient(), emailMessage.subject(),
                emailMessage.body());
    }

    @Test
    void publish_WhenAllRetriesFail_ShouldSendMessageToDeadLetterQueue() {

        doThrow(new RuntimeException("Permanent SMTP error")).when(emailService).sendEmail(emailMessage.recipient(),
                emailMessage.subject(), emailMessage.body());

        emailMessageProducer.publish(emailMessage);

        Message deadLetterMessage = rabbitTemplate.receive(RabbitMQConfig.EMAIL_DLQ, 10000);

        assertNotNull(deadLetterMessage);
        assertTrue(new String(deadLetterMessage.getBody(), java.nio.charset.StandardCharsets.UTF_8)
                .contains(emailMessage.recipient()));
        assertNotNull(deadLetterMessage.getMessageProperties().getHeaders().get("x-death"));
        verify(emailService, timeout(5000).times(4)).sendEmail(emailMessage.recipient(), emailMessage.subject(),
                emailMessage.body());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
            "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
    })
    @EnableRabbit
    @Import({
            RabbitMQConfig.class,
            EmailMessageProducer.class,
            EmailMessageConsumer.class
    })
    static class TestApplication {
    }
}
