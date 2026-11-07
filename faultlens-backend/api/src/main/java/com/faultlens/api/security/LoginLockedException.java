package com.faultlens.api.security;

import com.faultlens.common.exception.FaultLensException;
import java.time.Duration;

/** Too many failed logins for this username; answered with 429 and {@code Retry-After}. */
public class LoginLockedException extends FaultLensException {
    private final Duration retryAfter;

    public LoginLockedException(Duration retryAfter) {
        super("Too many failed login attempts; try again in " + retryAfter.toMinutes() + " minutes",
                "LOGIN_LOCKED");
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
