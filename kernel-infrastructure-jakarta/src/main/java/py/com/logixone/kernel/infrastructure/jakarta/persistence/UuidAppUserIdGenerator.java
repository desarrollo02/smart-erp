package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.application.security.port.AppUserIdGenerator;

@ApplicationScoped
public class UuidAppUserIdGenerator implements AppUserIdGenerator {

    @Override
    public AppUserId nextId() {
        return new AppUserId(UUID.randomUUID());
    }
}
