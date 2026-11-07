package com.faultlens.api.notification;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * SSRF guard for user-supplied webhook URLs (any role may configure one for themselves).
 *
 * <p>Only {@code https} URLs without credentials whose host resolves exclusively to public
 * addresses are accepted, unless private hosts are explicitly allowed for on-prem receivers.
 * Checked when the URL is saved and again before every send (DNS can change in between);
 * redirects are never followed.
 */
public final class WebhookUrls {
    private WebhookUrls() {
    }

    public static boolean isAllowed(String url, boolean allowPrivateHosts) {
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (RuntimeException ex) {
            return false;
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getRawUserInfo() != null) {
            return false;
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".internal")) {
            return allowPrivateHosts;
        }
        if (allowPrivateHosts) {
            return true;
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (!isPublic(address)) {
                    return false;
                }
            }
            return addresses.length > 0;
        } catch (UnknownHostException ex) {
            return false;
        }
    }

    static boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        byte[] b = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = b[0] & 0xff;
            int second = b[1] & 0xff;
            return !(first == 0
                    || (first == 100 && second >= 64 && second <= 127) // carrier-grade NAT
                    || (first == 192 && second == 0 && (b[2] & 0xff) == 0) // IETF protocol assignments
                    || (first == 198 && (second == 18 || second == 19)) // benchmarking
                    || first >= 240); // reserved, broadcast
        }
        if (address instanceof Inet6Address) {
            int first = b[0] & 0xff;
            if ((first & 0xfe) == 0xfc) { // unique local fc00::/7
                return false;
            }
            // IPv4-mapped ::ffff:a.b.c.d
            boolean mapped = true;
            for (int i = 0; i < 10; i++) {
                mapped &= b[i] == 0;
            }
            if (mapped && (b[10] & 0xff) == 0xff && (b[11] & 0xff) == 0xff) {
                try {
                    return isPublic(InetAddress.getByAddress(new byte[] {b[12], b[13], b[14], b[15]}));
                } catch (UnknownHostException ex) {
                    return false;
                }
            }
        }
        return true;
    }
}
