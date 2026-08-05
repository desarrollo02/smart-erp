package py.com.logixone.web.security;

import jakarta.enterprise.context.RequestScoped;
import java.util.UUID;

/** Server-generated correlation; client headers are not accepted as authority. */
@RequestScoped
public class RequestCorrelation {

    private final String value = UUID.randomUUID().toString();

    public String value() {
        return value;
    }
}
