package com.mustafa.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AccessReviewDecisionRequest {
    @NotNull
    private Decision decision;

    public enum Decision {
        APPROVE,
        REJECT
    }
}
