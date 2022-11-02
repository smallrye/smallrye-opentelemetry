package io.smallrye.opentelemetry.implementation.config;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;

import io.smallrye.opentelemetry.api.OpenTelemetryConfig;

@Singleton
public class OpenTelemetryConfigProducer {
    @Inject
    Config config;

    @Produces
    @Singleton
    public OpenTelemetryConfig produces() {
        return new OpenTelemetryConfig() {
            @Override
            public Map<String, String> properties() {
                Map<String, String> properties = new HashMap<>();
                for (String propertyName : config.getPropertyNames()) {
                    if (propertyName.startsWith("otel.") || propertyName.startsWith("OTEL_")) {
                        config.getOptionalValue(propertyName, String.class).ifPresent(
                                value -> properties.put(propertyName, value));
                    }
                }
                return properties;
            }
        };
    }
}
