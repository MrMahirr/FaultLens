package com.faultlens.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ApiKeyTokensTest {

    @Test
    void generatesWellFormedTokensWithMatchingHash() {
        ApiKeyTokens.Generated key = ApiKeyTokens.generate();

        assertThat(key.token()).matches("flk_[0-9A-Za-z]{8}_[0-9A-Za-z]{32}");
        assertThat(ApiKeyTokens.prefixOf(key.token())).contains(key.prefix());
        assertThat(key.hash()).hasSize(64).isEqualTo(ApiKeyTokens.hash(key.token()));
        assertThat(ApiKeyTokens.matches(key.token(), key.hash())).isTrue();
        assertThat(ApiKeyTokens.display(key.prefix())).isEqualTo("flk_" + key.prefix() + "_…");
    }

    @Test
    void hashNeverContainsTheToken() {
        ApiKeyTokens.Generated key = ApiKeyTokens.generate();

        assertThat(key.hash()).doesNotContain(key.prefix()).doesNotContain(key.token().substring(13));
    }

    @Test
    void rejectsMalformedTokensAndWrongSecrets() {
        ApiKeyTokens.Generated key = ApiKeyTokens.generate();
        String wrongSecret = key.token().substring(0, key.token().length() - 1)
                + (key.token().endsWith("a") ? "b" : "a");

        assertThat(ApiKeyTokens.matches(wrongSecret, key.hash())).isFalse();
        assertThat(ApiKeyTokens.matches(key.token(), null)).isFalse();
        assertThat(ApiKeyTokens.prefixOf("flk_short")).isEmpty();
        assertThat(ApiKeyTokens.prefixOf(null)).isEmpty();
        assertThat(ApiKeyTokens.prefixOf(key.token().replace("flk_", "xyz_"))).isEmpty();
        assertThat(ApiKeyTokens.prefixOf(key.token().substring(0, 12) + "-" + key.token().substring(13))).isEmpty();
        assertThat(ApiKeyTokens.looksLikeKey("eyJhbGciOi")).isFalse();
    }

    @Test
    void tokensAreUnique() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            tokens.add(ApiKeyTokens.generate().token());
        }
        assertThat(tokens).hasSize(1000);
    }
}
