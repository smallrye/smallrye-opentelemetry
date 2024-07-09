package io.smallrye.opentelemetry.api;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.Function;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;

public class OpenTelemetryBuilderGetter implements Function<OpenTelemetryConfig, AutoConfiguredOpenTelemetrySdkBuilder> {
    @Override
    public AutoConfiguredOpenTelemetrySdkBuilder apply(final OpenTelemetryConfig config) {
        AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder();

        ClassLoader contextClassLoader = SecuritySupport.getContextClassLoader();
        if (contextClassLoader != null) {
            builder.setServiceClassLoader(contextClassLoader);
        }

        if (System.getSecurityManager() == null) {
            return getOpenTelemetryBuilder(builder, config);
        }

        return AccessController.doPrivileged(
                (PrivilegedAction<AutoConfiguredOpenTelemetrySdkBuilder>) () -> getOpenTelemetryBuilder(builder, config));
    }

    private AutoConfiguredOpenTelemetrySdkBuilder getOpenTelemetryBuilder(final AutoConfiguredOpenTelemetrySdkBuilder builder,
            final OpenTelemetryConfig config) {
        return builder.addPropertiesSupplier(config::properties);
    }
}
