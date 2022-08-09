package io.smallrye.opentelemetry.implementation.config.traces.otlp;

import static io.smallrye.opentelemetry.implementation.config.traces.otlp.OtlpExporterRuntimeConfig.Constants.DEFAULT_GRPC_BASE_URL;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.opentelemetry.implementation.config.traces.TracesExporterConfig;

public interface OtlpExporterTracesConfig extends TracesExporterConfig {

    /**
     * OTLP Exporter specific. Will be concatenated after otel.exporter.otlp.endpoint.
     * <p>
     * The old Quarkus configuration used the opentelemetry.tracer.exporter.otlp.endpoint system property
     * to define the full endpoint starting with http or https.
     * If the old property is set, we will use it until the transition period ends.
     * <p>
     * Default value is {@value Constants#DEFAULT_GRPC_PATH}
     */
    //    @WithConverter(TrimmedStringConverter.class)
    @WithDefault(Constants.DEFAULT_GRPC_PATH)
    @Override
    Optional<String> endpoint();

    /**
     * See {@link OtlpExporterTracesConfig#endpoint()}
     */
    @Deprecated
    //    @WithConverter(TrimmedStringConverter.class)
    @WithDefault("${quarkus.opentelemetry.tracer.exporter.otlp.endpoint:" + DEFAULT_GRPC_BASE_URL + Constants.DEFAULT_GRPC_PATH
            + "}")
    @WithName("legacy-endpoint")
    Optional<String> legacyEndpoint();

    /**
     * Key-value pairs to be used as headers associated with gRPC requests.
     * The format is similar to the {@code OTEL_EXPORTER_OTLP_HEADERS} environment variable,
     * a list of key-value pairs separated by the "=" character.
     * See <a href=
     * "https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/exporter.md#specifying-headers-via-environment-variables">
     * Specifying headers</a> for more details.
     */
    @Override
    Map<String, String> headers();

    /**
     * Sets the maximum time to wait for the collector to process an exported batch of spans. If
     * unset, defaults to {@value OtelConnectionConfig.Constants#DEFAULT_TIMEOUT_SECS}s.
     */
    @Override
    @WithDefault("10S")
    Duration timeout();

    /**
     * From <a href=
     * "https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/exporter.md#configuration-options">
     * the OpenTelemetry Protocol Exporter configuration options</a>
     */
    class Constants {
        public static final String DEFAULT_GRPC_PATH = "v1/traces";
    }
}
