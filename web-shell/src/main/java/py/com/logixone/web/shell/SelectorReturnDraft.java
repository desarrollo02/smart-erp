package py.com.logixone.web.shell;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Filters the bounded, non-secret field types that the shell may retain briefly. */
final class SelectorReturnDraft {

    private static final int MAX_INPUTS = 96;
    private static final int MAX_VALUE_LENGTH = 2048;
    private static final int MAX_TOTAL_LENGTH = 16_384;
    private static final int MAX_ENCODED_LENGTH = 32_768;

    private SelectorReturnDraft() {
    }

    static Map<String, String> retain(
            Map<String, String> candidates, Set<String> allowedInputIds) {
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(allowedInputIds, "allowedInputIds");
        if (allowedInputIds.size() > MAX_INPUTS) {
            throw new IllegalArgumentException("Too many selector return inputs");
        }

        Map<String, String> retained = new LinkedHashMap<>();
        int totalLength = 0;
        for (String inputId : allowedInputIds) {
            Objects.requireNonNull(inputId, "allowed input id");
            if (!candidates.containsKey(inputId)) {
                continue;
            }
            String value = Objects.requireNonNullElse(candidates.get(inputId), "");
            if (value.length() > MAX_VALUE_LENGTH
                    || value.codePoints().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException("Invalid selector return input");
            }
            totalLength += inputId.length() + value.length();
            if (totalLength > MAX_TOTAL_LENGTH) {
                throw new IllegalArgumentException("Selector return draft is too large");
            }
            retained.put(inputId, value);
        }
        return Map.copyOf(retained);
    }

    static Map<String, String> decode(String encodedDraft) {
        if (encodedDraft == null || encodedDraft.isBlank()) {
            return Map.of();
        }
        if (encodedDraft.length() > MAX_ENCODED_LENGTH) {
            throw new IllegalArgumentException("Encoded selector return draft is too large");
        }
        Map<String, String> decoded = new LinkedHashMap<>();
        for (String pair : encodedDraft.split("&", -1)) {
            int separator = pair.indexOf('=');
            if (separator <= 0) {
                throw new IllegalArgumentException("Invalid encoded selector return draft");
            }
            String key = URLDecoder.decode(
                    pair.substring(0, separator), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(
                    pair.substring(separator + 1), StandardCharsets.UTF_8);
            if (!key.matches("[a-z][a-z0-9_]{0,63}")
                    || decoded.putIfAbsent(key, value) != null
                    || decoded.size() > MAX_INPUTS) {
                throw new IllegalArgumentException("Invalid encoded selector return input");
            }
        }
        return Map.copyOf(decoded);
    }
}
