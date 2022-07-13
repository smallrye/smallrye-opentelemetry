package io.smallrye.opentelemetry.implementation.config;

import io.smallrye.config.WithDefault;
import io.smallrye.opentelemetry.implementation.config.OpenTelemetryRuntimeConfig.ExporterType;

import java.util.Map;
import java.util.Optional;

import static io.smallrye.opentelemetry.implementation.config.OpenTelemetryRuntimeConfig.ExporterType.Constants.*;

public interface OtelConnectionConfig {

    /**
     * Exporter specific
     */
    String endpoint();

    /**
     * Sets the certificate chain to use for verifying servers when TLS is enabled. The {@code byte[]}
     * should contain an X.509 certificate collection in PEM format. If not set, TLS connections will
     * use the system default trusted certificates.
     */
    Optional<Byte[]> certificate();

    /**
     * Sets ths client key and the certificate chain to use for verifying client when TLS is enabled.
     * The key must be PKCS8, and both must be in PEM format.
     */
    Optional<ClientTlsConfig> client();

    /**
     * Add header to request. Optional.
     */
    Optional<Map<String, String>> headers();

    /**
     * Sets the method used to compress payloads. If unset, compression is disabled. Currently
     * supported compression methods include "gzip" and "none".
     */
    Optional<CompressionType> compression();

    /**
     * Sets the maximum time to wait for the collector to process an exported batch of spans. If
     * unset, defaults to {@value Constants#DEFAULT_TIMEOUT_SECS}s.
     */
    @WithDefault("10")
    Integer timeout();

    /**
     * OTLP defines the encoding of telemetry data and the protocol used to exchange data between the client and the server.
     */
    @WithDefault(OTLP_VALUE)
    ExporterType protocol(); // FIXME Depending on the protocol the endpoint will be different

    public interface ClientTlsConfig {

        byte[] key();

        byte[] certificate();
    }

    enum CompressionType {
        GZIP("gzip"),
        NONE("none");
        private String value;

        CompressionType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    class Constants {
        public static final String DEFAULT_TIMEOUT_SECS = "10";
    }
}
