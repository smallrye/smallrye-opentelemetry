package io.smallrye.opentelemetry.implementation.exporters;

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.smallrye.opentelemetry.api.OpenTelemetryConfig;

public final class OtlpExporterUtil {
    public static final String PROTOCOL_GRPC = "grpc";
    public static final String PROTOCOL_HTTP_PROTOBUF = "http/protobuf";

    public static final String OTLP_GRPC_ENDPOINT = "http://localhost:4317";
    public static final String OTLP_HTTP_PROTOBUF_ENDPOINT = "http://localhost:4318";

    public static final String OTEL_EXPORTER_OTLP_ENDPOINT = "otel.exporter.otlp.endpoint";
    public static final String OTEL_EXPORTER_OTLP_TRACES_PROTOCOL = "otel.exporter.otlp.traces.protocol";

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
}
