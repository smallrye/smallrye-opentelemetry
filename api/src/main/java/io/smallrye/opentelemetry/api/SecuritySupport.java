package io.smallrye.opentelemetry.api;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * SecuritySupport for the io.smallrye.opentelemetry.api package.
 * <p>
 * Do not move. Do not change class and method visibility to avoid being called from other
 * {@link java.security.CodeSource}s, thus granting privilege escalation to external code.
 */
class SecuritySupport {
    private SecuritySupport() {
        // Forbid inheritance!
    }

    static ClassLoader getContextClassLoader() {
        if (System.getSecurityManager() == null) {
            return Thread.currentThread().getContextClassLoader();
        } else {
            return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> {
                ClassLoader classLoader = null;
                try {
                    classLoader = Thread.currentThread().getContextClassLoader();
                } catch (SecurityException ex) {
                    // Ignore
                }
                return classLoader;
            });
        }
    }
}
