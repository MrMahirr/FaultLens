package com.faultlens.processor.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MessageNormalizerTest {
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "GET /api/v1/orders/123 200 in 45ms | GET /api/v<n>/orders/<n> <n> in <n>ms",
            "User u4242 logged in from 10.0.3.17 | User u<n> logged in from <ip>",
            "upstream 172.18.0.4:8080 timed out | upstream <ip> timed out",
            "request 3f2b8c1e-9a7d-4e21-b6aa-0c5d2e1f9b77 failed | request <uuid> failed",
            "mail to ops@example.com bounced | mail to <email> bounced",
            "at 2026-11-05T10:00:00.123Z job ran | at <ts> job ran",
            "object 0x7ffe2a3c freed, sha 9031263abcdef0 | object <hex> freed, sha <hex>",
            "Cannot invoke \"Order.getId()\"   because order is null | Cannot invoke \"Order.getId()\" because order is null",
    })
    void replacesVariablePartsWithPlaceholders(String input, String expected) {
        assertThat(MessageNormalizer.normalize(input)).isEqualTo(expected);
    }
}
