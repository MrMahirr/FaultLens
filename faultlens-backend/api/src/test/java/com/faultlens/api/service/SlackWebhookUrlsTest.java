package com.faultlens.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SlackWebhookUrlsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://hooks.slack.com/services/T000/B000/XXXX",
            "https://HOOKS.slack.com/services/T000/B000/XXXX",
            "https://hooks.slack.com:443/services/T000/B000/XXXX",
            "https://hooks.slack.com/workflows/T000/A000/123/abc",
            "https://hooks.slack.com/triggers/T000/123/abc"
    })
    void acceptsSlackWebhooks(String url) {
        assertThat(SlackWebhookUrls.isAllowed(url)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "http://hooks.slack.com/services/T000/B000/XXXX",          // plain http
            "https://hooks.slack.com.evil.com/services/T/B/X",          // suffix trick
            "https://evil.com/hooks.slack.com/services/T/B/X",          // host in path
            "https://hooks.slack.com@169.254.169.254/services/T/B/X",   // userinfo trick
            "https://user@hooks.slack.com/services/T/B/X",              // userinfo
            "https://hooks.slack.com:8443/services/T/B/X",              // other port
            "https://hooks.slack.com/api/chat.postMessage",             // not a webhook path
            "https://hooks.slack.com/services/../../admin",             // traversal
            "http://169.254.169.254/latest/meta-data/",                 // cloud metadata
            "http://localhost:3500/actuator/shutdown",                  // internal service
            "file:///etc/passwd",
            "not a url"
    })
    void rejectsEverythingElse(String url) {
        assertThat(SlackWebhookUrls.isAllowed(url)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://169.254.169.254/latest/meta-data/", "http://localhost:3500/actuator"})
    void senderRefusesNonSlackUrlWithoutMakingARequest(String url) {
        SlackNotificationService service = new SlackNotificationService(new ObjectMapper());

        assertThat(service.sendNotification(url, "hello")).isFalse();
    }
}
