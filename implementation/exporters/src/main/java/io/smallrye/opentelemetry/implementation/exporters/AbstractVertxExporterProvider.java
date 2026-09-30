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

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.export.GrpcSender;
import io.opentelemetry.sdk.common.export.HttpSender;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.opentelemetry.senders.VertxGrpcSender;
import io.smallrye.opentelemetry.senders.VertxHttpSender;
import io.vertx.core.Vertx;

public abstract class AbstractVertxExporterProvider {
    private final String signalType;
    private final String exporterName;

    private static final Logger logger = Logger.getLogger(AbstractVertxExporterProvider.class.getName());
    private static Vertx SUPPLIED_VERTX;

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

    /**
     * Stores a server-provided Vertx for constructing exporters.
     *
     * @param vertx the server-managed Vertx instance
     */
    public static void withVertx(Vertx vertx) {
        SUPPLIED_VERTX = vertx;
    }

    /**
     * Resolves Vertx from the provided instance, CDI, or a new instance, in that order.
     * A server build supplies its instance because CDI is not available during server startup.
     *
     * @param config the exporter configuration used for CDI selection
     * @return the Vertx instance used by the exporter
     */
    Vertx getVertx(ConfigProperties config) {
        if (SUPPLIED_VERTX != null) {
            return SUPPLIED_VERTX;
        }
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

    protected GrpcSender createGrpcSender(ConfigProperties config, String grpcEndpointPath) throws URISyntaxException {
        URI baseUri = new URI(getOtlpEndpoint(config, OTLP_GRPC_ENDPOINT, signalType));
        return new VertxGrpcSender(
                baseUri,
                grpcEndpointPath,
                getCompression(config, signalType),
                getTimeout(config, signalType),
                OtlpExporterUtil.populateTracingExportHttpHeaders(),
                new HttpClientOptionsConsumer(config, baseUri, signalType),
                getVertx(config));
    }

    protected HttpSender createHttpSender(ConfigProperties config, String httpEndpointPath) throws URISyntaxException {
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
