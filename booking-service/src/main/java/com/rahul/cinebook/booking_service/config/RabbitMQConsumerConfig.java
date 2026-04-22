package com.rahul.cinebook.booking_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConsumerConfig {
    public static final String PAYMENT_SUCCESS_QUEUE = "payment_success_queue";

    @Bean
    public Queue paymentSuccessQueue(){
        return new Queue(PAYMENT_SUCCESS_QUEUE);
    }
    @Bean
    public MessageConverter jsonMessageConvertor(){
        return new JacksonJsonMessageConverter();
    }
}
