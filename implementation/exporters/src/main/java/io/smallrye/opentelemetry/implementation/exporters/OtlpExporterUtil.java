package io.smallrye.opentelemetry.implementation.exporters;

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.opentelemetry.exporter.otlp.internal.OtlpUserAgent;

final class OtlpExporterUtil {
    static final String OTEL_EXPORTER_OTLP_ENDPOINT = "otel.exporter.otlp.endpoint";
    static final String OTEL_EXPORTER_OTLP_PROTOCOL = "otel.exporter.otlp.protocol";
    static final String OTEL_EXPORTER_OTLP_TIMEOUT = "otel.exporter.otlp.timeout";
    static final String OTEL_EXPORTER_OTLP_TRACES_ENDPOINT = "otel.exporter.otlp.traces.endpoint";
    static final String OTEL_EXPORTER_OTLP_TRACES_PROTOCOL = "otel.exporter.otlp.traces.protocol";
    static final String PROTOCOL_GRPC = "grpc";
    static final String PROTOCOL_HTTP_PROTOBUF = "http/protobuf";

    private OtlpExporterUtil() {
    }

    static int getPort(URI uri) {
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
        OtlpUserAgent.addUserAgentHeader(headersMap::put);
        return headersMap;
    }

    static boolean isHttps(URI uri) {
        return "https".equals(uri.getScheme().toLowerCase(Locale.ROOT));
    }
}
