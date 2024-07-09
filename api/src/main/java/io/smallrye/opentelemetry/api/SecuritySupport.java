package io.smallrye.opentelemetry.api;

import java.security.AccessController;
import java.security.PrivilegedAction;

public class SecuritySupport {
    private SecuritySupport() {
        throw new UnsupportedOperationException();
    }

    public static ClassLoader getContextClassLoader() {
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
