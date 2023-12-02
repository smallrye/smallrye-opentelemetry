package io.smallrye.opentelemetry.implementation.exporters;

import static io.smallrye.opentelemetry.implementation.exporters.OtlpExporterUtil.OTEL_EXPORTER_OTLP_PROTOCOL;
import static io.smallrye.opentelemetry.implementation.exporters.OtlpExporterUtil.OTEL_EXPORTER_OTLP_TIMEOUT;
import static io.smallrye.opentelemetry.implementation.exporters.OtlpExporterUtil.OTEL_EXPORTER_OTLP_TRACES_PROTOCOL;
import static io.smallrye.opentelemetry.implementation.exporters.OtlpExporterUtil.PROTOCOL_GRPC;
import static io.smallrye.opentelemetry.implementation.exporters.OtlpExporterUtil.PROTOCOL_HTTP_PROTOBUF;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.internal.http.HttpExporter;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.vertx.core.Vertx;

public class VertxExporterProvider implements ConfigurableSpanExporterProvider {

    protected static final String EXPORTER_NAME = "otlp";
    protected static final String OTLP_GRPC_ENDPOINT = "http://localhost:4317";
    protected static final String OTLP_HTTP_PROTOBUF_ENDPOINT = "http://localhost:4318";

    @Override
    public SpanExporter createExporter(final ConfigProperties config) {
        try {
            final String otlpProtocol = config.getString(OTEL_EXPORTER_OTLP_TRACES_PROTOCOL,
                    config.getString(OTEL_EXPORTER_OTLP_PROTOCOL, PROTOCOL_GRPC));

            switch (otlpProtocol) {
                case PROTOCOL_GRPC:
                    return new VertxGrpcExporter(
                            EXPORTER_NAME, // use the same as OTel does
                            "span", // use the same as OTel does
                            MeterProvider::noop,
                            new URI(getOtlpEndpoint(config, OTLP_GRPC_ENDPOINT)),
                            true,
                            config.getDuration(OTEL_EXPORTER_OTLP_TIMEOUT, Duration.ofSeconds(10)),
                            OtlpExporterUtil.populateTracingExportHttpHeaders(),
                            Vertx.vertx());
                case PROTOCOL_HTTP_PROTOBUF:
                    return new VertxHttpExporter(
                            new HttpExporter<>(
                                    EXPORTER_NAME, // use the same as OTel does
                                    "span", // use the same as OTel does
                                    new VertxHttpExporter.VertxHttpSender(
                                            new URI(getOtlpEndpoint(config, OTLP_HTTP_PROTOBUF_ENDPOINT)),
                                            true,
                                            config.getDuration(OTEL_EXPORTER_OTLP_TIMEOUT, Duration.ofSeconds(10)),
                                            OtlpExporterUtil.populateTracingExportHttpHeaders(),
                                            "application/x-protobuf",
                                            Vertx.vertx()),
                                    MeterProvider::noop,
                                    false));
                default:
                    throw new RuntimeException("Unsupported OTLP protocol: " + otlpProtocol);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        // Using the same name as the OpenTelemetry SDK ("otlp") allows us to override its definition, providing our
        // Vertx-based exporters without any additional changes to the user's application.
        return EXPORTER_NAME;
    }

    /**
     * Gets the OTLP traces endpoint, if defined. If it is not, it returns the OTLP endpoint. If that is not defined,
     * it returns defaultEndpoint.
     *
     * @param config
     * @param defaultEndpoint The default endpoint for the desired protocol
     * @return
     */
    private static String getOtlpEndpoint(ConfigProperties config, String defaultEndpoint) {
        return config.getString(OtlpExporterUtil.OTEL_EXPORTER_OTLP_TRACES_ENDPOINT,
                config.getString(OtlpExporterUtil.OTEL_EXPORTER_OTLP_ENDPOINT, defaultEndpoint));
    }
}
