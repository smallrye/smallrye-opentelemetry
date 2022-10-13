package io.smallrye.opentelemetry.api;

import java.util.Optional;

public interface OpenTelemetryConfig {
    String INSTRUMENTATION_NAME = "io.smallrye.opentelemetry";

    String INSTRUMENTATION_VERSION = Optional.ofNullable(OpenTelemetryConfig.class.getPackage().getImplementationVersion())
            .orElse("SNAPSHOT");
}
