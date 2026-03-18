package com.mustafa.dto.request;

import com.mustafa.entity.Account.Currency;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateAccountRequest {

    @NotNull(message = "Para birimi boş bırakılamaz (Örn: TRY, USD, EUR)")
    private Currency currency;
}