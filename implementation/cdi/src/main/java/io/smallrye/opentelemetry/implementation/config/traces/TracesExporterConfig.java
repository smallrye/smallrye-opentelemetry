package io.smallrye.opentelemetry.implementation.config.traces;

import io.smallrye.config.WithDefault;
import io.smallrye.opentelemetry.implementation.config.OtelConnectionConfig;

import java.util.Optional;

/**
 * Path: otel.exporter.traces
 */
public interface TracesExporterConfig extends OtelConnectionConfig {

    /**
     * Enable tracing with OpenTelemetry.
     * <p>
     * This property is not available in the Open Telemetry SDK. It's Quarkus specific.
     * <p>
     * Support for tracing will be enabled if OpenTelemetry support is enabled
     * and either this value is true, or this value is unset.
     */
    @WithDefault("true")
    Optional<Boolean> enabled();

}
