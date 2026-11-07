package com.faultlens.api.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Format and hashing of API key tokens: {@code flk_<prefix>_<secret>} with an 8-character prefix
 * and a 32-character secret (base62, about 190 bits). The stored hash is SHA-256 of the whole
 * token; a fast hash is fine because the token is long and random (nothing to guess from).
 */
public final class ApiKeyTokens {
    public static final String PREFIX = "flk_";
    static final int PREFIX_LENGTH = 8;
    static final int SECRET_LENGTH = 32;
    static final int TOKEN_LENGTH = PREFIX.length() + PREFIX_LENGTH + 1 + SECRET_LENGTH;
    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private ApiKeyTokens() {
    }

    public record Generated(String token, String prefix, String hash) {
    }

    public static Generated generate() {
        String prefix = random(PREFIX_LENGTH);
        String token = PREFIX + prefix + "_" + random(SECRET_LENGTH);
        return new Generated(token, prefix, hash(token));
    }

    public static boolean looksLikeKey(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    /** The lookup prefix of a well-formed token, empty for anything else. */
    public static Optional<String> prefixOf(String token) {
        if (token == null || token.length() != TOKEN_LENGTH || !token.startsWith(PREFIX)
                || token.charAt(PREFIX.length() + PREFIX_LENGTH) != '_') {
            return Optional.empty();
        }
        return Optional.of(token.substring(PREFIX.length(), PREFIX.length() + PREFIX_LENGTH));
    }

    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    /** Constant-time comparison against a stored hex hash. */
    public static boolean matches(String token, String expectedHash) {
        return expectedHash != null && MessageDigest.isEqual(
                hash(token).getBytes(StandardCharsets.US_ASCII),
                expectedHash.getBytes(StandardCharsets.US_ASCII));
    }

    /** Masked form for display, for example {@code flk_Ab12Cd34_…}. */
    public static String display(String prefix) {
        return PREFIX + prefix + "_…";
    }

    private static String random(int length) {
        char[] out = new char[length];
        for (int i = 0; i < length; i++) {
            out[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(out);
    }
}
