package com.faultlens.api.service;

import java.net.URI;
import java.util.List;
import java.util.Locale;

/**
 * Allow-list for Slack incoming-webhook URLs.
 *
 * <p>The backend POSTs to these URLs, so accepting any URL would let a user make the server call
 * internal services or cloud metadata endpoints (SSRF). Only {@code https://hooks.slack.com} with a
 * webhook path is accepted.
 */
public final class SlackWebhookUrls {
    static final String HOST = "hooks.slack.com";
    private static final List<String> PATH_PREFIXES = List.of("/services/", "/workflows/", "/triggers/");

    private SlackWebhookUrls() {
    }

    /**
     * Returns true when the URL is an https Slack webhook URL on the default port.
     */
    public static boolean isAllowed(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(url.trim());
            String host = uri.getHost();
            String path = uri.getRawPath();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && host != null
                    && HOST.equals(host.toLowerCase(Locale.ROOT))
                    && uri.getRawUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() == 443)
                    && path != null
                    && !path.contains("..")
                    && PATH_PREFIXES.stream().anyMatch(path::startsWith);
        } catch (Exception ex) {
            return false;
        }
    }
}
