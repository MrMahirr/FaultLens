package com.faultlens.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    /** Claim set while the user still has to replace a bootstrap/default password. */
    public static final String CLAIM_PASSWORD_CHANGE = "pwd_change";

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${faultlens.jwt.secret}") String secret,
            @Value("${faultlens.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
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
