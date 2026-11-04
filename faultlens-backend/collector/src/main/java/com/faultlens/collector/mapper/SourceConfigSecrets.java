package com.faultlens.collector.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Masks secret values in a log source's JSON config before it leaves the API, and restores them
 * when a client sends the mask back unchanged on update.
 *
 * <p>File paths ({@code privateKeyPath}, {@code kubeconfigPath}) are not secrets and stay visible
 * so users can edit them.
 */
public final class SourceConfigSecrets {
    /** Placeholder returned instead of a secret; sending it back keeps the stored value. */
    public static final String MASK = "••••••••";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String[] SECRET_MARKERS = {
            "password", "passphrase", "secret", "token", "apikey", "credential", "privatekey"
    };

    private SourceConfigSecrets() {
    }

    /**
     * Returns true when a config key holds a secret value.
     */
    public static boolean isSecretKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        if (normalized.endsWith("path") || normalized.endsWith("file")) {
            return false;
        }
        for (String marker : SECRET_MARKERS) {
            if (normalized.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Replaces non-empty secret values with {@link #MASK}. Configs that are not a JSON object are
     * replaced by {@code "{}"}, because their contents cannot be inspected safely.
     */
    public static String mask(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return configJson;
        }
        ObjectNode root = parseObject(configJson);
        if (root == null) {
            return "{}";
        }
        maskRecursively(root);
        return write(root);
    }

    /**
     * Copies stored secrets into {@code incomingJson} wherever the client sent {@link #MASK} back.
     * A mask with no stored counterpart is dropped rather than saved as a literal password.
     */
    public static String restoreMasked(String incomingJson, String storedJson) {
        if (incomingJson == null || incomingJson.isBlank() || !incomingJson.contains(MASK)) {
            return incomingJson;
        }
        ObjectNode incoming = parseObject(incomingJson);
        if (incoming == null) {
            return incomingJson;
        }
        ObjectNode stored = storedJson == null ? null : parseObject(storedJson);
        restoreRecursively(incoming, stored);
        return write(incoming);
    }

    private static void maskRecursively(ObjectNode node) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode value = field.getValue();
            if (value.isObject()) {
                maskRecursively((ObjectNode) value);
            } else if (isSecretKey(field.getKey()) && !value.isNull() && !value.asText().isEmpty()) {
                field.setValue(MAPPER.getNodeFactory().textNode(MASK));
            }
        }
    }

    private static void restoreRecursively(ObjectNode incoming, ObjectNode stored) {
        Iterator<Map.Entry<String, JsonNode>> fields = incoming.properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode value = field.getValue();
            JsonNode storedValue = stored == null ? null : stored.get(field.getKey());
            if (value.isObject()) {
                restoreRecursively((ObjectNode) value, storedValue != null && storedValue.isObject()
                        ? (ObjectNode) storedValue : null);
            } else if (value.isTextual() && MASK.equals(value.asText())) {
                if (storedValue != null && !storedValue.isNull()) {
                    field.setValue(storedValue);
                } else {
                    fields.remove();
                }
            }
        }
    }

    private static ObjectNode parseObject(String json) {
        try {
            JsonNode node = MAPPER.readTree(json);
            return node instanceof ObjectNode objectNode ? objectNode : null;
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private static String write(JsonNode node) {
        try {
            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize source config", ex);
        }
    }
}
