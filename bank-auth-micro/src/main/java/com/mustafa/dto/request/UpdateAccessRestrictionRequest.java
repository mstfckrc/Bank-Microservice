package com.mustafa.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAccessRestrictionRequest {
    @NotNull
    private Boolean restricted;
}
