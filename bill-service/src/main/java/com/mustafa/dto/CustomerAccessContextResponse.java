package com.mustafa.dto;

public record CustomerAccessContextResponse(
        String role,
        String approvalStatus,
        String accessRestrictionStatus) {
}
