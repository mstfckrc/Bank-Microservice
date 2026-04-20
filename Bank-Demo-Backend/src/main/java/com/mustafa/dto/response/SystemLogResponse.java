package com.mustafa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLogResponse {
    private String timestamp;
    private String level;
    private String appName;
    private String threadName;
    private String loggerName;
    private String message;
}