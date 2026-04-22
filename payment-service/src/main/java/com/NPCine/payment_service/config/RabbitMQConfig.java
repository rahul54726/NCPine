package com.NPCine.payment_service.config;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public   static  final String PAYMENT_EXCHANGE = "payment_exchange";
    public static  final String PAYMENT_SUCCESS_QUEUE = "payment_success_queue";
    public static  final String PAYMENT_SUCCESS_ROUTING_KEY = "payment_success";

    @Bean
    public Queue queue(){
        return new Queue(PAYMENT_SUCCESS_QUEUE);
    }
    @Bean
    public DirectExchange exchange(){
        return new DirectExchange(PAYMENT_EXCHANGE);
    }
    @Bean
    public Binding binding(Queue queue , DirectExchange exchange){
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(PAYMENT_SUCCESS_ROUTING_KEY);
    }
    @Bean
    public MessageConverter converter(){
        return new JacksonJsonMessageConverter();
    }
}
