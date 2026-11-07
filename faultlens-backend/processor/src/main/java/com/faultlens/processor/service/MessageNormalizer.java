package com.faultlens.processor.service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Turns a log line into its "shape" for grouping (P08): variable parts become placeholders so
 * {@code GET /orders/123 200 in 45ms} and {@code GET /orders/987 200 in 12ms} share one group.
 *
 * <p>Before this, the raw message was fingerprinted, so nearly every line created its own group
 * (95% of groups held a single entry) and every insert took a row lock — the main ingest cost
 * and the source of deadlocks between consumers.
 */
public final class MessageNormalizer {
    private record Rule(Pattern pattern, String replacement) {
    }

    private static final List<Rule> RULES = List.of(
            // ISO-8601 / log timestamps
            rule("\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})?", "<ts>"),
            rule("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b", "<uuid>"),
            rule("\\b[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+\\b", "<email>"),
            rule("\\b\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d{1,5})?\\b", "<ip>"),
            rule("\\b0x[0-9a-fA-F]+\\b", "<hex>"),
            // long hex ids / hashes (at least one digit so plain words stay)
            rule("\\b(?=[0-9a-fA-F]*\\d)[0-9a-fA-F]{12,}\\b", "<hex>"),
            // numbers, also when glued to letters: u4242, 45ms, worker-3, v1.2.3
            rule("\\d+(?:\\.\\d+)*", "<n>"),
            rule("\\s+", " "));

    private static Rule rule(String regex, String replacement) {
        return new Rule(Pattern.compile(regex), replacement);
    }

    private MessageNormalizer() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String out = text;
        for (Rule rule : RULES) {
            out = rule.pattern().matcher(out).replaceAll(rule.replacement());
        }
        return out.strip();
    }
}
