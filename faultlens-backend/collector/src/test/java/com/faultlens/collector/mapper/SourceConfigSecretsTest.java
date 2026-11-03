package com.faultlens.collector.mapper;

import static com.faultlens.collector.mapper.SourceConfigSecrets.MASK;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SourceConfigSecretsTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void classifiesSecretKeys() {
        assertThat(SourceConfigSecrets.isSecretKey("password")).isTrue();
        assertThat(SourceConfigSecrets.isSecretKey("sshPassword")).isTrue();
        assertThat(SourceConfigSecrets.isSecretKey("key_passphrase")).isTrue();
        assertThat(SourceConfigSecrets.isSecretKey("apiKey")).isTrue();
        assertThat(SourceConfigSecrets.isSecretKey("bearer-token")).isTrue();
        assertThat(SourceConfigSecrets.isSecretKey("privateKey")).isTrue();

        assertThat(SourceConfigSecrets.isSecretKey("privateKeyPath")).isFalse();
        assertThat(SourceConfigSecrets.isSecretKey("kubeconfigPath")).isFalse();
        assertThat(SourceConfigSecrets.isSecretKey("tokenFile")).isFalse();
        assertThat(SourceConfigSecrets.isSecretKey("host")).isFalse();
        assertThat(SourceConfigSecrets.isSecretKey("username")).isFalse();
    }

    @Test
    void masksSecretsIncludingNestedOnes() throws Exception {
        JsonNode masked = json.readTree(SourceConfigSecrets.mask(
                "{\"host\":\"h\",\"password\":\"p\",\"auth\":{\"token\":\"t\",\"user\":\"u\"},\"apiKey\":\"\"}"));

        assertThat(masked.get("host").asText()).isEqualTo("h");
        assertThat(masked.get("password").asText()).isEqualTo(MASK);
        assertThat(masked.get("auth").get("token").asText()).isEqualTo(MASK);
        assertThat(masked.get("auth").get("user").asText()).isEqualTo("u");
        // empty values stay empty so the UI can tell "not set" from "set"
        assertThat(masked.get("apiKey").asText()).isEmpty();
    }

    @Test
    void hidesConfigThatIsNotAJsonObject() {
        assertThat(SourceConfigSecrets.mask("password=hunter2")).isEqualTo("{}");
        assertThat(SourceConfigSecrets.mask("[\"x\"]")).isEqualTo("{}");
        assertThat(SourceConfigSecrets.mask(null)).isNull();
    }

    @Test
    void restoresStoredSecretWhenMaskIsSentBack() throws Exception {
        String restored = SourceConfigSecrets.restoreMasked(
                "{\"host\":\"new-host\",\"password\":\"" + MASK + "\"}",
                "{\"host\":\"old-host\",\"password\":\"s3cret\"}");

        JsonNode node = json.readTree(restored);
        assertThat(node.get("host").asText()).isEqualTo("new-host");
        assertThat(node.get("password").asText()).isEqualTo("s3cret");
    }

    @Test
    void dropsMaskWithoutStoredCounterpartInsteadOfSavingItAsPassword() throws Exception {
        JsonNode node = json.readTree(SourceConfigSecrets.restoreMasked(
                "{\"host\":\"h\",\"password\":\"" + MASK + "\"}", "{\"host\":\"h\"}"));

        assertThat(node.has("password")).isFalse();
    }

    @Test
    void leavesConfigWithoutMaskUntouched() {
        String incoming = "{\"password\":\"brand-new\"}";
        assertThat(SourceConfigSecrets.restoreMasked(incoming, "{\"password\":\"old\"}")).isSameAs(incoming);
    }

    @Test
    void maskThenRestoreRoundTripsTheOriginal() throws Exception {
        String stored = "{\"host\":\"h\",\"username\":\"u\",\"password\":\"p@ss\",\"privateKeyPath\":\"/k\"}";

        String roundTrip = SourceConfigSecrets.restoreMasked(SourceConfigSecrets.mask(stored), stored);

        assertThat(json.readTree(roundTrip)).isEqualTo(json.readTree(stored));
    }
}
