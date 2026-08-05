package py.com.logixone.plugin.api;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable qualified identity for a capability, permission or menu contribution. */
public record ContributionId(String value) implements Comparable<ContributionId> {

    private static final int MAX_LENGTH = 128;
    private static final Pattern VALID_VALUE =
            Pattern.compile("[a-z][a-z0-9_]*(?:[.:][a-z][a-z0-9_]*)*");

    public ContributionId {
        Objects.requireNonNull(value, "value");
        if (value.length() > MAX_LENGTH || !VALID_VALUE.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid contribution id: " + value);
        }
    }

    @Override
    public int compareTo(ContributionId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
