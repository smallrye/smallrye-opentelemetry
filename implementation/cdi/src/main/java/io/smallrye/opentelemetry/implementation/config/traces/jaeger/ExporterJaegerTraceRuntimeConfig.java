package io.smallrye.opentelemetry.implementation.config.traces.jaeger;

import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.opentelemetry.implementation.config.OtelConnectionConfig;
import io.smallrye.opentelemetry.implementation.config.traces.TracesExporterConfig;

@ConfigMapping(prefix = "otel.exporter.jaeger")
//@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ExporterJaegerTraceRuntimeConfig extends OtelConnectionConfig {
    // TODO default Jaeger endpoint

    /**
     * Sets the Jaeger endpoint to connect to. If unset, defaults to {@value Constants#DEFAULT_GRPC_ENDPOINT}.
     */
    @Override
    //    @WithConverter(TrimmedStringConverter.class)
    Optional<String> endpoint(); // TODO Optional?

    /**
     * Jaeger traces exporter configuration
     */
    TracesExporterConfig traces();
    // TODO metrics();
    // TODO logs();

    public static class Constants {
        public static final String DEFAULT_GRPC_ENDPOINT = "http://localhost:14250";
        public static final String DEFAULT_HTTP_ENDPOINT = "http://localhost:14268/api/traces";
    }
}
