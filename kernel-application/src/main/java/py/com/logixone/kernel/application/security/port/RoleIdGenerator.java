package py.com.logixone.kernel.application.security.port;

import py.com.logixone.kernel.domain.security.RoleId;

@FunctionalInterface
public interface RoleIdGenerator {

    RoleId nextId();
}
