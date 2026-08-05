package py.com.logixone.web.security;

/** Generic browser-facing access failure with no internal diagnostic details. */
public final class TrustedWebAccessException extends RuntimeException {

    private final int status;

    private TrustedWebAccessException(int status, String message) {
        super(message);
        this.status = status;
    }

    public static TrustedWebAccessException unauthorized() {
        return new TrustedWebAccessException(401, "unauthorized");
    }

    public static TrustedWebAccessException forbidden() {
        return new TrustedWebAccessException(403, "forbidden");
    }

    public int status() {
        return status;
    }
}
