package com.mustafa.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty; // 🚀 BU İMPORTU UNUTMA
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountValidationResponse {

    private String ownerIdentityNumber;

    // 🚀 NOKTA ATIŞI: Java'daki adı "isActive" kalsın, ama JSON'da okurken ve yazarken de KESİNLİKLE "isActive" ara diyoruz!
    @JsonProperty("isActive")
    private boolean isActive;
}