package com.mustafa.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountValidationResponse {
    private String ownerIdentityNumber; // Kasanın sahibi kim?

    @JsonProperty("isActive")
    private boolean isActive;           // Kasa açık mı?
}