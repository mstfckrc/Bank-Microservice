package com.mustafa.dto.response;

public record CustomerAccessContextResponse(
        String role,
        String approvalStatus,
        String accessRestrictionStatus) {
}
