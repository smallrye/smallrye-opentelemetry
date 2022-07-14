package io.smallrye.opentelemetry.implementation.config.traces.jaeger;

import io.smallrye.config.ConfigMapping;
import io.smallrye.opentelemetry.implementation.config.OtelConnectionConfig;

@ConfigMapping(prefix = "otel.exporter.jaeger")
public interface ExporterJaegerTraceConfig extends OtelConnectionConfig {
    // TODO default Jaeger endpoint
}
