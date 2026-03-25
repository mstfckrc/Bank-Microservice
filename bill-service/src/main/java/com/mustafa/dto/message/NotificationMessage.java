package com.mustafa.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private String identityNumber;
    private String destination;
    private String subject;
    private String content;
    private NotificationType notificationType;

    public enum NotificationType {
        EMAIL, SMS, PUSH_NOTIFICATION, SYSTEM_ALERT
    }
}