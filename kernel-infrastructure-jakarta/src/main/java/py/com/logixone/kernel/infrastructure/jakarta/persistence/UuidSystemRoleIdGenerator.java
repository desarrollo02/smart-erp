package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import py.com.logixone.kernel.application.security.system.port.SystemRoleIdGenerator;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;

@ApplicationScoped
public class UuidSystemRoleIdGenerator implements SystemRoleIdGenerator {

    @Override
    public SystemRoleId nextId() {
        return new SystemRoleId(UUID.randomUUID());
    }
}
