package io.smallrye.opentelemetry.implementation.exporters;

import static io.smallrye.opentelemetry.implementation.exporters.OtlpExporterUtil.OTEL_EXPORTER_OTLP_ENDPOINT;
import static io.smallrye.opentelemetry.implementation.exporters.OtlpExporterUtil.OTLP_GRPC_ENDPOINT;
import static io.smallrye.opentelemetry.implementation.exporters.OtlpExporterUtil.OTLP_HTTP_PROTOBUF_ENDPOINT;
import static io.smallrye.opentelemetry.implementation.exporters.OtlpExporterUtil.PROTOCOL_GRPC;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
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
    private static final String OTEL_EXPORTER_OTLP_PROTOCOL = "otel.exporter.otlp.protocol";
    private static final String OTEL_EXPORTER_OTLP_SIGNAL_PROTOCOL = "otel.exporter.otlp.%s.protocol";

    private static final String OTEL_EXPORTER_OTLP_TIMEOUT = "otel.exporter.otlp.timeout";
    private static final String OTEL_EXPORTER_OTLP_SIGNAL_TIMEOUT = "otel.exporter.otlp.%s.timeout";

    private static final String OTEL_EXPORTER_OTLP_SIGNAL_ENDPOINT = "otel.exporter.otlp.%s.endpoint";

    private static final String OTEL_EXPORTER_OTLP_COMPRESSION = "otel.exporter.otlp.compression";
    private static final String OTEL_EXPORTER_OTLP_SIGNAL_COMPRESSION = "otel.exporter.otlp.%s.compression";

    private static final String MIMETYPE_PROTOBUF = "application/x-protobuf";

    private static final String OTEL_EXPORTER_VERTX_CDI_QUALIFIER = "otel.exporter.vertx.cdi.identifier";

    private static final Logger logger = Logger.getLogger(AbstractVertxExporterProvider.class.getName());

    private final String signalType;
    private final String exporterName;

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
        return new VertxGrpcSender<>(
                new URI(getOtlpEndpoint(config, OTLP_GRPC_ENDPOINT)),
                grpcEndpointPath,
                getCompression(config),
                getTimeout(config),
                OtlpExporterUtil.populateTracingExportHttpHeaders(),
                getVertx(config));
    }

    protected VertxHttpSender createHttpSender(ConfigProperties config, String httpEndpointPath) throws URISyntaxException {
        return new VertxHttpSender(
                new URI(getOtlpEndpoint(config, OTLP_HTTP_PROTOBUF_ENDPOINT)),
                httpEndpointPath,
                getCompression(config),
                getTimeout(config),
                OtlpExporterUtil.populateTracingExportHttpHeaders(),
                MIMETYPE_PROTOBUF,
                getVertx(config));
    }

    protected IllegalArgumentException buildUnsupportedProtocolException(String protocol) {
        String signalProperty = String.format(OTEL_EXPORTER_OTLP_SIGNAL_ENDPOINT, signalType);

        return new IllegalArgumentException(String.format("Unsupported OTLP protocol %s specified. ", protocol) +
                String.format("Please check the `%s` and/or '%s' properties", signalProperty, OTEL_EXPORTER_OTLP_ENDPOINT));
    }

    /**
     * Given the OpenTelemetry config, lookup a value using the given keys, stopping with the first non-null value. If
     * no keys are found, return the defaultValue. Since the OpenTelemetry API offers signal-specific settings, as well
     * as one overarching property key for a variety of configuration options, this allows the caller to specify
     * signal-specific keys to search, then defaulting to the "top-level" property, then finally defaulting to a
     * SmallRye-specific default value.
     *
     * @param config OpenTelemetry config
     * @param defaultValue The default value for the property.
     * @param keys The keys to iterate over
     * @return either the configured or default value
     */
    protected String getConfig(ConfigProperties config, String defaultValue, String... keys) {
        String value = null;
        for (String key : keys) {
            value = config.getString(key);
            if (value != null) {
                break;
            }
        }

        return value != null ? value : defaultValue;
    }

    /**
     * Determine the wire protocol for sending signal data to the remote
     *
     * @param config OpenTelemetry configuration
     * @return either the configured or default value
     */
    protected String getProtocol(ConfigProperties config) {
        // The otel API uses "span" and "traces" in various places, so we need to modify that case here
        String signalKey = signalType.replace("span", "traces");

        return getConfig(config, PROTOCOL_GRPC,
                String.format(OTEL_EXPORTER_OTLP_SIGNAL_PROTOCOL, signalKey), OTEL_EXPORTER_OTLP_PROTOCOL);
    }

    /**
     * Determine whether to enable compression
     *
     * @param config OpenTelemetry configuration
     * @return either the configured or default value
     */
    protected boolean getCompression(ConfigProperties config) {
        return Boolean.parseBoolean(getConfig(config, "true",
                String.format(OTEL_EXPORTER_OTLP_SIGNAL_COMPRESSION, signalType),
                OTEL_EXPORTER_OTLP_COMPRESSION));
    }

    /**
     * Return timeout, in seconds, for sending data to the remote
     *
     * @param config OpenTelemetry configuration
     * @return either the configured or default value
     */
    protected Duration getTimeout(ConfigProperties config) {
        return Duration.ofSeconds(Integer.parseInt(
                getConfig(config, "10",
                        String.format(OTEL_EXPORTER_OTLP_SIGNAL_TIMEOUT, signalType), OTEL_EXPORTER_OTLP_TIMEOUT)));
    }

    /**
     * Gets the OTLP traces endpoint, if defined. If it is not, it returns the OTLP endpoint. If that is not defined,
     * it returns defaultEndpoint.
     *
     * @param config OpenTelemetry configuration
     * @param defaultEndpoint The default endpoint for the desired protocol
     * @return either the configured or default value
     */
    protected String getOtlpEndpoint(ConfigProperties config, String defaultEndpoint) {
        return getConfig(config, defaultEndpoint,
                String.format(OTEL_EXPORTER_OTLP_SIGNAL_ENDPOINT, signalType),
                OTEL_EXPORTER_OTLP_ENDPOINT);
    }
}
