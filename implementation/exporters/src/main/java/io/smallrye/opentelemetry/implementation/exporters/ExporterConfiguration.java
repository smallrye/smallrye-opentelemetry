package io.smallrye.opentelemetry.implementation.exporters;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.TrustOptions;

/**
 * SPI for framework-specific exporter configuration.
 * <p>
 * SmallRye provides a default implementation using OTel {@code ConfigProperties}.
 * Frameworks like Quarkus or WildFly provide their own implementations
 * with framework-specific TLS and proxy infrastructure.
 */
public interface ExporterConfiguration {

    URI getEndpoint();

    String getProtocol();

    Duration getTimeout();

    boolean isCompressionEnabled();

    Map<String, String> getHeaders();

    String getTemporalityPreference();

    String getDefaultHistogramAggregation();

    Vertx getVertx();

    // TLS configuration

    KeyCertOptions getKeyCertOptions();

    TrustOptions getTrustOptions();

    boolean isTrustAll();

    // Proxy configuration

    boolean isProxyEnabled();

    Optional<String> getProxyHost();

    OptionalInt getProxyPort();

    Optional<String> getProxyUsername();

    Optional<String> getProxyPassword();

    default int getPort() {
        URI uri = getEndpoint();
        int originalPort = uri.getPort();
        if (originalPort > -1) {
            return originalPort;
        }
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return 443;
        }
        return 80;
    }

    /**
     * Returns an HttpClientOptions customizer that configures TLS, proxy,
     * and other transport-level settings based on this configuration.
     */
    default Consumer<HttpClientOptions> getHttpClientOptionsCustomizer() {
        return new HttpClientOptionsConsumer(this);
    }
}
