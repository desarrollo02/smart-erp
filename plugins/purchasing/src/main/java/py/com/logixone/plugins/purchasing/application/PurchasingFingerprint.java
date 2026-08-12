package py.com.logixone.plugins.purchasing.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

final class PurchasingFingerprint {
    private PurchasingFingerprint() {
    }

    static String of(String operation, Object command) {
        String canonical = Objects.requireNonNull(operation, "operation") + "|"
                + Objects.requireNonNull(command, "command").toString();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by Java", impossible);
        }
    }
}
