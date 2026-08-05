package py.com.logixone.kernel.application.security.system.port;

import py.com.logixone.kernel.domain.security.system.SystemRoleId;

@FunctionalInterface
public interface SystemRoleIdGenerator {

    SystemRoleId nextId();
}
