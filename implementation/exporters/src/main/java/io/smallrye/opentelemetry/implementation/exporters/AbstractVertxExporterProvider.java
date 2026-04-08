package io.smallrye.opentelemetry.implementation.exporters;

import static io.smallrye.opentelemetry.implementation.exporters.Constants.MIMETYPE_PROTOBUF;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_ENDPOINT;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_SIGNAL_ENDPOINT;

import java.net.URI;

import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.internal.grpc.GrpcExporter;
import io.opentelemetry.exporter.internal.http.HttpExporter;
import io.opentelemetry.sdk.common.InternalTelemetryVersion;
import io.opentelemetry.sdk.common.internal.ComponentId;
import io.opentelemetry.sdk.common.internal.StandardComponentId;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.opentelemetry.implementation.exporters.sender.VertxGrpcSender;
import io.smallrye.opentelemetry.implementation.exporters.sender.VertxHttpSender;

public abstract class AbstractVertxExporterProvider {
    private final String signalType;
    private final String exporterName;
    private ExporterConfiguration configuration;

    private static final Logger logger = Logger.getLogger(AbstractVertxExporterProvider.class);

    public AbstractVertxExporterProvider(String signalType, String exporterName) {
        this.signalType = signalType;
        this.exporterName = exporterName;
    }

    public String getName() {
        return exporterName;
    }

    protected String getSignalType() {
        return signalType;
    }

    protected ExporterConfiguration getConfiguration() {
        if (configuration == null) {
            configuration = CDI.current()
                    .select(ExporterConfiguration.class, Identifier.Literal.of(signalType))
                    .get();
        }
        return configuration;
    }

    protected GrpcExporter createGrpcExporter(String grpcEndpointPath,
            StandardComponentId.ExporterType exporterType) {
        ExporterConfiguration config = getConfiguration();
        URI baseUri = config.getEndpoint();
        return new GrpcExporter(
                new VertxGrpcSender(
                        grpcEndpointPath,
                        config),
                InternalTelemetryVersion.LATEST,
                ComponentId.generateLazy(exporterType),
                MeterProvider::noop,
                baseUri);
    }

    protected HttpExporter createHttpExporter(String httpEndpointPath,
            StandardComponentId.ExporterType exporterType) {
        ExporterConfiguration config = getConfiguration();
        URI baseUri = config.getEndpoint();
        return new HttpExporter(
                ComponentId.generateLazy(exporterType),
                new VertxHttpSender(
                        httpEndpointPath,
                        MIMETYPE_PROTOBUF,
                        config),
                MeterProvider::noop,
                InternalTelemetryVersion.LATEST,
                baseUri,
                false);
    }

    protected IllegalArgumentException buildUnsupportedProtocolException(String protocol) {
        String signalProperty = String.format(OTEL_EXPORTER_OTLP_SIGNAL_ENDPOINT, signalType);

        return new IllegalArgumentException(String.format("Unsupported OTLP protocol %s specified. ", protocol) +
                String.format("Please check the `%s` and/or '%s' properties", signalProperty, OTEL_EXPORTER_OTLP_ENDPOINT));
    }
}
