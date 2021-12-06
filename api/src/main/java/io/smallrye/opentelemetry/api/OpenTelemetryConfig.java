package io.smallrye.opentelemetry.api;

public interface OpenTelemetryConfig {
    String INSTRUMENTATION_NAME = "io.smallrye.opentelemetry";

    String INSTRUMENTATION_VERSION = OpenTelemetryConfig.class.getPackage().getImplementationVersion();
}
