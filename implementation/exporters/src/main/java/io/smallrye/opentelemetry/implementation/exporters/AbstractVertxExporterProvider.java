package io.smallrye.opentelemetry.implementation.exporters;

import static io.smallrye.opentelemetry.implementation.exporters.Constants.MIMETYPE_PROTOBUF;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_ENDPOINT;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_SIGNAL_ENDPOINT;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_VERTX_CDI_QUALIFIER;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTLP_GRPC_ENDPOINT;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTLP_HTTP_PROTOBUF_ENDPOINT;
import static io.smallrye.opentelemetry.implementation.exporters.OtlpExporterUtil.getCompression;
import static io.smallrye.opentelemetry.implementation.exporters.OtlpExporterUtil.getOtlpEndpoint;
import static io.smallrye.opentelemetry.implementation.exporters.OtlpExporterUtil.getTimeout;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.internal.grpc.GrpcExporter;
import io.opentelemetry.exporter.internal.http.HttpExporter;
import io.opentelemetry.exporter.internal.marshal.Marshaler;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.opentelemetry.implementation.exporters.sender.VertxGrpcSender;
import io.smallrye.opentelemetry.implementation.exporters.sender.VertxHttpSender;
import io.vertx.core.Vertx;

public abstract class AbstractVertxExporterProvider<T extends Marshaler> {
    private final String signalType;
    private final String exporterName;

    private static final Logger logger = Logger.getLogger(AbstractVertxExporterProvider.class.getName());

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

    protected GrpcExporter<T> createGrpcExporter(ConfigProperties config, String grpcEndpointPath) throws URISyntaxException {
        return new GrpcExporter<>(getName(), getSignalType(), createGrpcSender(config, grpcEndpointPath), MeterProvider::noop);
    }

    protected HttpExporter<T> createHttpExporter(ConfigProperties config, String httpEndpointPath) throws URISyntaxException {
        return new HttpExporter<>(getName(), getSignalType(), createHttpSender(config, httpEndpointPath), MeterProvider::noop,
                false);//TODO: this will be enhanced in the future
    }

    /**
     * If the CDI qualifier is specified in the config, it tries to get it from CDI, and if CDI does not provide such
     * an instance on the specified qualifier, it will log some WARNING messages and return a new Vertx instance.
     * If the CDI qualifier is not specified in the config, it creates a new Vertx instance.
     */
    private Vertx getVertx(ConfigProperties config) {
        String cdiQualifier = config.getString(OTEL_EXPORTER_VERTX_CDI_QUALIFIER);
        if (cdiQualifier != null && !cdiQualifier.isEmpty()) {
            Instance<Vertx> vertxCDI = CDI.current().select(Vertx.class, Identifier.Literal.of(cdiQualifier));
            if (vertxCDI != null && vertxCDI.isResolvable()) {
                return vertxCDI.get();
            } else {
                logger.log(Level.WARNING, "The Vertx instance with CDI qualifier @Identifier(\"{0}\") is not resolvable.",
                        cdiQualifier);
            }
        }
        logger.log(Level.INFO, "Create a new Vertx instance");
        return Vertx.vertx();
    }

    protected VertxGrpcSender<T> createGrpcSender(ConfigProperties config, String grpcEndpointPath) throws URISyntaxException {
        URI baseUri = new URI(getOtlpEndpoint(config, OTLP_GRPC_ENDPOINT, signalType));
        return new VertxGrpcSender<>(
                signalType,
                baseUri,
                grpcEndpointPath,
                getCompression(config, signalType),
                getTimeout(config, signalType),
                OtlpExporterUtil.populateTracingExportHttpHeaders(),
                new HttpClientOptionsConsumer(config, baseUri, signalType),
                getVertx(config));
    }

    protected VertxHttpSender createHttpSender(ConfigProperties config, String httpEndpointPath) throws URISyntaxException {
        URI baseUri = new URI(getOtlpEndpoint(config, OTLP_HTTP_PROTOBUF_ENDPOINT, signalType));
        return new VertxHttpSender(
                baseUri,
                httpEndpointPath,
                getCompression(config, signalType),
                getTimeout(config, signalType),
                OtlpExporterUtil.populateTracingExportHttpHeaders(),
                MIMETYPE_PROTOBUF,
                new HttpClientOptionsConsumer(config, baseUri, signalType),
                getVertx(config));
    }

    protected IllegalArgumentException buildUnsupportedProtocolException(String protocol) {
        String signalProperty = String.format(OTEL_EXPORTER_OTLP_SIGNAL_ENDPOINT, signalType);

        return new IllegalArgumentException(String.format("Unsupported OTLP protocol %s specified. ", protocol) +
                String.format("Please check the `%s` and/or '%s' properties", signalProperty, OTEL_EXPORTER_OTLP_ENDPOINT));
    }
}
