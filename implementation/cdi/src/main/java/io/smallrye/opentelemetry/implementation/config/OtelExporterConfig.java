package io.smallrye.opentelemetry.implementation.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.opentelemetry.implementation.config.traces.TracesExporterConfig;

@ConfigMapping(prefix = "otel.exporter.otlp")
public interface OtelExporterConfig extends OtelConnectionConfig {
    TracesExporterConfig traces();
    // TODO metrics();
    // TODO logs();
}
