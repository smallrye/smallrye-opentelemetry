package io.smallrye.opentelemetry.implementation.config.traces.otlp;

import io.smallrye.config.ConfigMapping;
import io.smallrye.opentelemetry.implementation.config.OtelConnectionConfig;

@ConfigMapping(prefix = "otel.exporter.otlp")
public interface ExporterOtlpTraceConfig extends OtelConnectionConfig {
}
