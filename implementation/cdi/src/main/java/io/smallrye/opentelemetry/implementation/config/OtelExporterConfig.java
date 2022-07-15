package io.smallrye.opentelemetry.implementation.config;

import io.smallrye.opentelemetry.implementation.config.traces.TracesExporterConfig;

public interface OtelExporterConfig extends OtelConnectionConfig {
    TracesExporterConfig traces();
    // TODO metrics();
    // TODO logs();
}
