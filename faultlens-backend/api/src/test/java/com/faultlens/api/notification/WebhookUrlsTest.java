package com.faultlens.api.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WebhookUrlsTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "http://93.184.215.14/hook", // not https
            "https://user:pass@93.184.215.14/hook", // credentials
            "https://localhost/hook",
            "https://metadata.google.internal/computeMetadata/v1/",
            "https://127.0.0.1/hook",
            "https://10.0.0.5/hook",
            "https://172.16.3.4/hook",
            "https://192.168.1.10/hook",
            "https://169.254.169.254/latest/meta-data/", // cloud metadata
            "https://100.64.0.1/hook", // CGNAT
            "https://0.0.0.0/hook",
            "https://[::1]/hook",
            "https://[fd00::1]/hook",
            "https://[::ffff:127.0.0.1]/hook",
            "https:///nohost",
            "not a url"})
    void rejectsNonPublicOrNonHttpsTargets(String url) {
        assertThat(WebhookUrls.isAllowed(url, false)).as(url).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://93.184.215.14/hook", "https://[2606:2800:21f:cb07:6820:80da:af6b:8b2c]/x"})
    void acceptsPublicHttpsTargets(String url) {
        assertThat(WebhookUrls.isAllowed(url, false)).as(url).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://10.0.0.5/hook", "https://alerts.internal/hook"})
    void privateHostsCanBeAllowedForOnPremReceivers(String url) {
        assertThat(WebhookUrls.isAllowed(url, true)).isTrue();
        // ...but never plain http
        assertThat(WebhookUrls.isAllowed(url.replace("https", "http"), true)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.8.8.8", "1.1.1.1"})
    void publicAddressesArePublic(String ip) throws Exception {
        assertThat(WebhookUrls.isPublic(InetAddress.getByName(ip))).isTrue();
    }
}
