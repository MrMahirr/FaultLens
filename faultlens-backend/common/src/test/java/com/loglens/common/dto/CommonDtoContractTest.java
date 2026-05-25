package com.loglens.common.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.junit.jupiter.api.Test;

class CommonDtoContractTest {
    @Test
    void restEnvelopeUsesSnakeCaseJsonNaming() {
        JsonNaming naming = ApiResponse.class.getAnnotation(JsonNaming.class);

        assertThat(naming).isNotNull();
        assertThat(naming.value()).isEqualTo(PropertyNamingStrategies.SnakeCaseStrategy.class);
    }

    @Test
    void logGroupDtoUsesSnakeCaseJsonNaming() {
        JsonNaming naming = LogGroupDto.class.getAnnotation(JsonNaming.class);

        assertThat(naming).isNotNull();
        assertThat(naming.value()).isEqualTo(PropertyNamingStrategies.SnakeCaseStrategy.class);
    }

    @Test
    void apiResponseFactoryPopulatesSuccessEnvelope() {
        ApiResponse<String> response = ApiResponse.ok("payload");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("payload");
        assertThat(response.getMessage()).isEqualTo("OK");
        assertThat(response.getTimestamp()).isNotNull();
    }
}
