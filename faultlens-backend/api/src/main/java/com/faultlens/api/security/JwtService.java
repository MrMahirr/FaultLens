package com.faultlens.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtService {
    /** Claim set while the user still has to replace a bootstrap/default password. */
    public static final String CLAIM_PASSWORD_CHANGE = "pwd_change";

    /** HS256 needs at least 256 bits of key material. */
    static final int MIN_SECRET_BYTES = 32;
    /** Secrets that were committed to this repository; anyone could forge tokens with them. */
    static final Set<String> PUBLISHED_SECRETS = Set.of(
            "faultlens-secret-change-in-production-needs-256-bits-minimum");

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${faultlens.jwt.secret:}") String secret,
            @Value("${faultlens.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(resolveSecret(secret));
        this.expirationMs = expirationMs;
    }

    /**
     * Fails fast on a published or too-short secret. When no secret is configured a random one is
     * generated for this process: nobody can forge tokens, but tokens do not survive a restart and
     * are not shared between instances, hence the warning.
     */
    static byte[] resolveSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            byte[] random = new byte[64];
            new SecureRandom().nextBytes(random);
            log.warn("JWT_SECRET is not set: using a random per-process signing key. Sessions end on "
                    + "restart and multiple instances will not accept each other's tokens. "
                    + "Set JWT_SECRET (e.g. `openssl rand -base64 48`).");
            return random;
        }
        if (PUBLISHED_SECRETS.contains(secret.trim())) {
            throw new IllegalStateException(
                    "JWT_SECRET is the example value from this repository; generate a new one (openssl rand -base64 48)");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes (got " + bytes.length + ")");
        }
        return bytes;
    }

    /**
     * Generates a JWT for the supplied username and roles.
     */
    public String generateToken(String username, List<String> roles) {
        return generateToken(username, roles, false);
    }

    /**
     * Generates a JWT; when {@code passwordChangeRequired} is true the token only grants access to
     * the password-change flow (enforced by {@link JwtAuthFilter}).
     */
    public String generateToken(String username, List<String> roles, boolean passwordChangeRequired) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(username)
                .claim("roles", roles);
        if (passwordChangeRequired) {
            builder.claim(CLAIM_PASSWORD_CHANGE, true);
        }
        return builder
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Parses and validates a token.
     */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /**
     * Returns true when the token is structurally valid, signed and not expired.
     */
    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Returns true when the token was issued for a user who must change their password first.
     */
    public static boolean requiresPasswordChange(Claims claims) {
        return Boolean.TRUE.equals(claims.get(CLAIM_PASSWORD_CHANGE, Boolean.class));
    }

    /**
     * Extracts the username from a valid token.
     */
    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    /**
     * Returns token expiration time for a freshly issued token.
     */
    public Instant expiresAt() {
        return Instant.now().plusMillis(expirationMs);
    }
}
