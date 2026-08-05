package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import py.com.logixone.kernel.application.security.port.RoleIdGenerator;
import py.com.logixone.kernel.domain.security.RoleId;

@ApplicationScoped
public class UuidRoleIdGenerator implements RoleIdGenerator {

    @Override
    public RoleId nextId() {
        return new RoleId(UUID.randomUUID());
    }
}
