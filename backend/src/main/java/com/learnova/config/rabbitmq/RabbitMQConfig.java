package com.learnova.config.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "learnova.exchange";

    public static final String EMAIL_QUEUE = "learnova.email.queue";

    public static final String EMAIL_ROUTING_KEY = "learnova.email.send";

    public static final String DLX_EXCHANGE = "learnova.dlx";

    public static final String EMAIL_DLQ = "learnova.email.dlq";

    public static final String EMAIL_DLQ_ROUTING_KEY = "learnova.email.dlq";

    @Bean
    public TopicExchange learnovaExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE).deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(EMAIL_DLQ_ROUTING_KEY).build();
    }

    @Bean
    public Queue emailDeadLetterQueue() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue()).to(learnovaExchange()).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding emailDeadLetterBinding() {
        return BindingBuilder.bind(emailDeadLetterQueue()).to(deadLetterExchange()).with(EMAIL_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
