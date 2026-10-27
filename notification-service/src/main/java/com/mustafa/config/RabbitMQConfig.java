package com.mustafa.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_QUEUE = "notification_queue";
    private static final String ERROR_EXCHANGE = "bank_notification_error_exchange";
    private static final String ERROR_ROUTING_KEY = "notification.failed";

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Declarables notificationBindings() {
        DirectExchange exchange = new DirectExchange("bank_exchange");
        Queue queue = new Queue(NOTIFICATION_QUEUE, true);
        DirectExchange errorExchange = new DirectExchange(ERROR_EXCHANGE);
        Queue errorQueue = new Queue("notification_failed_queue", true);
        return new Declarables(exchange, BindingBuilder.bind(queue).to(exchange).with("notification_routing_key"),
                errorExchange, errorQueue, BindingBuilder.bind(errorQueue).to(errorExchange).with(ERROR_ROUTING_KEY));
    }

    @Bean
    @Primary
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public MessageConverter rawNotificationMessageConverter() {
        return new MessageConverter() {
            @Override
            public Message toMessage(Object object, MessageProperties properties) {
                return new Message((byte[]) object, properties);
            }

            @Override
            public Object fromMessage(Message message) {
                return message.getBody();
            }
        };
    }

    @Bean
    public SmartInitializingSingleton notificationReturns(RabbitTemplate template) {
        return () -> template.setReturnsCallback(returned ->
                log.warn("Notification error queue publication returned; messageId={}",
                        returned.getMessage().getMessageProperties().getMessageId()));
    }

    @Bean
    public MessageRecoverer notificationRecoverer(RabbitTemplate template) {
        return (message, cause) -> {
            String messageId = message.getMessageProperties().getMessageId();
            try {
                CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                template.send(ERROR_EXCHANGE, ERROR_ROUTING_KEY, message, correlation);
                CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirm.isAck() || correlation.getReturned() != null) {
                    throw new IllegalStateException("Error queue publication was not accepted");
                }
                log.warn("Notification moved to error queue; messageId={}", messageId);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ImmediateRequeueAmqpException("Notification error queue unavailable");
            } catch (Exception exception) {
                log.error("Notification error queue unavailable; messageId={}, error={}",
                        messageId, exception.getClass().getSimpleName());
                throw new ImmediateRequeueAmqpException("Notification error queue unavailable");
            }
        };
    }
}
