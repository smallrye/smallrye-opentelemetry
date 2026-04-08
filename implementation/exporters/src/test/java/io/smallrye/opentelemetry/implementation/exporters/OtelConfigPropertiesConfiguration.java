package io.smallrye.opentelemetry.implementation.exporters;

import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_CERTIFICATE;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_CLIENT_KEY;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_SIGNAL_CERTIFICATE;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_SIGNAL_CLIENT_CERTIFICATE;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_SIGNAL_CLIENT_KEY;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTLP_GRPC_ENDPOINT;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTLP_HTTP_PROTOBUF_ENDPOINT;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.PROTOCOL_GRPC;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.SROTEL_PROXY_ENABLED;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.SROTEL_PROXY_HOST;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.SROTEL_PROXY_PASSWORD;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.SROTEL_PROXY_PORT;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.SROTEL_PROXY_USERNAME;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.SROTEL_TLS_TRUST_ALL;
import static io.smallrye.opentelemetry.implementation.exporters.OtlpExporterUtil.getConfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.TrustOptions;

/**
 * Per-signal implementation of {@link ExporterConfiguration} using OTel {@code ConfigProperties}.
 * All values are resolved eagerly at construction time.
 * Used by non-Quarkus consumers of SmallRye OpenTelemetry.
 */
public class OtelConfigPropertiesConfiguration implements ExporterConfiguration {

    private static final Logger logger = Logger.getLogger(OtelConfigPropertiesConfiguration.class);

    private final URI endpoint;
    private final String protocol;
    private final Duration timeout;
    private final boolean compressionEnabled;
    private final Map<String, String> headers;
    private final String temporalityPreference;
    private final String defaultHistogramAggregation;
    private final Vertx vertx;

    // TLS
    private final KeyCertOptions keyCertOptions;
    private final TrustOptions trustOptions;
    private final boolean trustAll;

    // Proxy
    private final boolean proxyEnabled;
    private final Optional<String> proxyHost;
    private final OptionalInt proxyPort;
    private final Optional<String> proxyUsername;
    private final Optional<String> proxyPassword;

    public OtelConfigPropertiesConfiguration(ConfigProperties config, String signalType) {
        this.protocol = OtlpExporterUtil.getProtocol(config, signalType);
        String defaultEndpoint = PROTOCOL_GRPC.equals(protocol) ? OTLP_GRPC_ENDPOINT : OTLP_HTTP_PROTOBUF_ENDPOINT;
        try {
            this.endpoint = new URI(OtlpExporterUtil.getOtlpEndpoint(config, defaultEndpoint, signalType));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid endpoint URI", e);
        }
        this.timeout = OtlpExporterUtil.getTimeout(config, signalType);
        this.compressionEnabled = OtlpExporterUtil.getCompression(config, signalType);
        this.headers = OtlpExporterUtil.populateTracingExportHttpHeaders();
        this.temporalityPreference = getConfig(config, "cumulative",
                "otel.exporter.otlp.metrics.temporality.preference");
        this.defaultHistogramAggregation = getConfig(config, "explicit_bucket_histogram",
                "otel.exporter.otlp.metrics.default.histogram.aggregation");
        this.vertx = resolveVertx(config);

        // TLS
        this.keyCertOptions = resolveKeyCertOptions(config, signalType);
        this.trustOptions = resolveTrustOptions(config, signalType);
        this.trustAll = Boolean.parseBoolean(getConfig(config, "false", SROTEL_TLS_TRUST_ALL));

        // Proxy
        this.proxyEnabled = Boolean.parseBoolean(getConfig(config, "false", SROTEL_PROXY_ENABLED));
        String host = getConfig(config, "", SROTEL_PROXY_HOST);
        this.proxyHost = host.isBlank() ? Optional.empty() : Optional.of(host);
        String port = getConfig(config, "", SROTEL_PROXY_PORT);
        this.proxyPort = port.isBlank() ? OptionalInt.empty() : OptionalInt.of(Integer.parseInt(port));
        String username = getConfig(config, "", SROTEL_PROXY_USERNAME);
        this.proxyUsername = username.isBlank() ? Optional.empty() : Optional.of(username);
        String password = getConfig(config, "", SROTEL_PROXY_PASSWORD);
        this.proxyPassword = password.isBlank() ? Optional.empty() : Optional.of(password);
    }

    @Override
    public URI getEndpoint() {
        return endpoint;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public Duration getTimeout() {
        return timeout;
    }

    @Override
    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String getTemporalityPreference() {
        return temporalityPreference;
    }

    @Override
    public String getDefaultHistogramAggregation() {
        return defaultHistogramAggregation;
    }

    @Override
    public Vertx getVertx() {
        return vertx;
    }

    @Override
    public KeyCertOptions getKeyCertOptions() {
        return keyCertOptions;
    }

    @Override
    public TrustOptions getTrustOptions() {
        return trustOptions;
    }

    @Override
    public boolean isTrustAll() {
        return trustAll;
    }

    @Override
    public boolean isProxyEnabled() {
        return proxyEnabled;
    }

    @Override
    public Optional<String> getProxyHost() {
        return proxyHost;
    }

    @Override
    public OptionalInt getProxyPort() {
        return proxyPort;
    }

    @Override
    public Optional<String> getProxyUsername() {
        return proxyUsername;
    }

    @Override
    public Optional<String> getProxyPassword() {
        return proxyPassword;
    }

    private static KeyCertOptions resolveKeyCertOptions(ConfigProperties config, String signalType) {
        String certificate = getConfig(config, "",
                String.format(OTEL_EXPORTER_OTLP_SIGNAL_CLIENT_CERTIFICATE, signalType),
                OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE);
        String key = getConfig(config, "",
                String.format(OTEL_EXPORTER_OTLP_SIGNAL_CLIENT_KEY, signalType),
                OTEL_EXPORTER_OTLP_CLIENT_KEY);

        if (certificate.isEmpty() && key.isEmpty()) {
            return null;
        }

        PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
        if (!certificate.isEmpty()) {
            pemKeyCertOptions.addCertPath(certificate);
        }
        if (!key.isEmpty()) {
            pemKeyCertOptions.addKeyPath(key);
        }
        return pemKeyCertOptions;
    }

    private static TrustOptions resolveTrustOptions(ConfigProperties config, String signalType) {
        String certificate = getConfig(config, "",
                String.format(OTEL_EXPORTER_OTLP_SIGNAL_CERTIFICATE, signalType),
                OTEL_EXPORTER_OTLP_CERTIFICATE);

        if (certificate.isEmpty()) {
            return null;
        }

        return new PemTrustOptions().addCertPath(certificate);
    }

    private static Vertx resolveVertx(ConfigProperties config) {
        String cdiQualifier = config.getString(Constants.OTEL_EXPORTER_VERTX_CDI_QUALIFIER);
        if (cdiQualifier != null && !cdiQualifier.isEmpty()) {
            Instance<Vertx> vertxCDI = CDI.current().select(Vertx.class, Identifier.Literal.of(cdiQualifier));
            if (vertxCDI != null && vertxCDI.isResolvable()) {
                return vertxCDI.get();
            } else {
                logger.warnf("The Vertx instance with CDI qualifier @Identifier(\"%s\") is not resolvable.",
                        cdiQualifier);
            }
        }
        logger.info("Create a new Vertx instance");
        return Vertx.vertx();
    }
}
