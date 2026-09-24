package com.mustafa.config;

import com.mustafa.dto.message.NotificationMessage;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RabbitMQConfigTest {

    @Test
    void notificationMessageSurvivesJsonRoundTrip() {
        NotificationMessage original = NotificationMessage.builder()
                .destination("customer@example.test")
                .subject("Payment received")
                .content("Your payment was received.")
                .identityNumber("*******1111")
                .notificationType(NotificationMessage.NotificationType.EMAIL)
                .build();
        MessageConverter converter = new RabbitMQConfig().jsonMessageConverter();

        Message amqpMessage = converter.toMessage(original, new MessageProperties());
        Object decoded = converter.fromMessage(amqpMessage);

        assertInstanceOf(NotificationMessage.class, decoded);
        assertEquals(original, decoded);
    }
}
