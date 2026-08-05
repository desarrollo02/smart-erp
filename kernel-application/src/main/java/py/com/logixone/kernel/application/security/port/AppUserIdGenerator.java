package py.com.logixone.kernel.application.security.port;

import py.com.logixone.kernel.api.security.AppUserId;

@FunctionalInterface
public interface AppUserIdGenerator {

    AppUserId nextId();
}
