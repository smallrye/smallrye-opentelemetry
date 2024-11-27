package io.smallrye.opentelemetry.implementation.exporters;

import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_COMPRESSION;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_PROTOCOL;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_SIGNAL_COMPRESSION;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_SIGNAL_ENDPOINT;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_SIGNAL_PROTOCOL;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_SIGNAL_TIMEOUT;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.smallrye.opentelemetry.api.OpenTelemetryConfig;

public final class OtlpExporterUtil {
    private OtlpExporterUtil() {
    }

    public static int getPort(URI uri) {
        int originalPort = uri.getPort();
        if (originalPort > -1) {
            return originalPort;
        }

        if (isHttps(uri)) {
            return 443;
        }
        return 80;
    }

    public static Map<String, String> populateTracingExportHttpHeaders() {
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put("User-Agent", OpenTelemetryConfig.INSTRUMENTATION_NAME + " " +
                OpenTelemetryConfig.INSTRUMENTATION_VERSION);
        return headersMap;
    }

    public static boolean isHttps(URI uri) {
        return "https".equals(uri.getScheme().toLowerCase(Locale.ROOT));
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
    public static String getConfig(ConfigProperties config, String defaultValue, String... keys) {
        return Arrays.stream(keys).map(config::getString)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(defaultValue);
    }

    /**
     * Determine the wire protocol for sending signal data to the remote
     *
     * @param config OpenTelemetry configuration
     * @return either the configured or default value
     */
    public static String getProtocol(ConfigProperties config, String signalType) {
        // The otel API uses "span" and "traces" in various places, so we need to modify that case here
        var signalKey = signalType.replace("span", "traces");

        return getConfig(config, Constants.PROTOCOL_GRPC,
                String.format(OTEL_EXPORTER_OTLP_SIGNAL_PROTOCOL, signalKey), OTEL_EXPORTER_OTLP_PROTOCOL);
    }

    /**
     * Determine whether to enable compression
     *
     * @param config OpenTelemetry configuration
     * @return either the configured or default value
     */
    public static boolean getCompression(ConfigProperties config, String signalType) {
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
    public static Duration getTimeout(ConfigProperties config, String signalType) {
        return Duration.ofSeconds(Integer.parseInt(
                getConfig(config, "10",
                        String.format(OTEL_EXPORTER_OTLP_SIGNAL_TIMEOUT, signalType), Constants.OTEL_EXPORTER_OTLP_TIMEOUT)));
    }

    /**
     * Gets the OTLP traces endpoint, if defined. If it is not, it returns the OTLP endpoint. If that is not defined,
     * it returns defaultEndpoint.
     *
     * @param config OpenTelemetry configuration
     * @param defaultEndpoint The default endpoint for the desired protocol
     * @return either the configured or default value
     */
    public static String getOtlpEndpoint(ConfigProperties config, String defaultEndpoint, String signalType) {
        return getConfig(config, defaultEndpoint,
                String.format(OTEL_EXPORTER_OTLP_SIGNAL_ENDPOINT, signalType),
                Constants.OTEL_EXPORTER_OTLP_ENDPOINT);
    }
}
