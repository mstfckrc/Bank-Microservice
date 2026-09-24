package com.mustafa.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrencyServiceImplTest {

    @Test
    void sameCurrencyReturnsOriginalAmount() {
        Double result = new CurrencyServiceImpl().convertAmount(125.75, "TRY", "try");

        assertEquals(Double.valueOf(125.75), result);
    }
}
