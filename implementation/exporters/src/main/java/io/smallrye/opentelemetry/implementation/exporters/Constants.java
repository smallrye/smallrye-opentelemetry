package io.smallrye.opentelemetry.implementation.exporters;

public class Constants {
    private Constants() {
    }

    public static final String PROTOCOL_GRPC = "grpc";
    public static final String PROTOCOL_HTTP_PROTOBUF = "http/protobuf";

    public static final String OTLP_GRPC_ENDPOINT = "http://localhost:4317";
    public static final String OTLP_HTTP_PROTOBUF_ENDPOINT = "http://localhost:4318";

    public static final String OTEL_EXPORTER_OTLP_ENDPOINT = "otel.exporter.otlp.endpoint";
    public static final String OTEL_EXPORTER_OTLP_TRACES_PROTOCOL = "otel.exporter.otlp.traces.protocol";

    static final String OTEL_EXPORTER_VERTX_CDI_QUALIFIER = "otel.exporter.vertx.cdi.identifier";

    static final String OTEL_EXPORTER_OTLP_CERTIFICATE = "otel.exporter.otlp.certificate";
    static final String OTEL_EXPORTER_OTLP_SIGNAL_CERTIFICATE = "otel.exporter.otlp.%s.certificate";

    static final String OTEL_EXPORTER_OTLP_CLIENT_KEY = "otel.exporter.otlp.client.key";
    static final String OTEL_EXPORTER_OTLP_SIGNAL_CLIENT_KEY = "otel.exporter.otlp.%s.client.key";

    static final String OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE = "otel.exporter.otlp.client.certificate";
    static final String OTEL_EXPORTER_OTLP_SIGNAL_CLIENT_CERTIFICATE = "otel.exporter.otlp.%s.client.certificate";

    static final String OTEL_EXPORTER_OTLP_PROTOCOL = "otel.exporter.otlp.protocol";
    static final String OTEL_EXPORTER_OTLP_SIGNAL_PROTOCOL = "otel.exporter.otlp.%s.protocol";

    static final String OTEL_EXPORTER_OTLP_TIMEOUT = "otel.exporter.otlp.timeout";
    static final String OTEL_EXPORTER_OTLP_SIGNAL_TIMEOUT = "otel.exporter.otlp.%s.timeout";

    static final String OTEL_EXPORTER_OTLP_SIGNAL_ENDPOINT = "otel.exporter.otlp.%s.endpoint";

    static final String OTEL_EXPORTER_OTLP_COMPRESSION = "otel.exporter.otlp.compression";
    static final String OTEL_EXPORTER_OTLP_SIGNAL_COMPRESSION = "otel.exporter.otlp.%s.compression";

    static final String MIMETYPE_PROTOBUF = "application/x-protobuf";

    static final String SROTEL_TLS_TRUST_ALL = "otel.exporter.tls.trustAll";
    // Proxy options
    static final String SROTEL_PROXY_ENABLED = "otel.exporter.proxy.enabled";
    static final String SROTEL_PROXY_USERNAME = "otel.exporter.proxy.username";
    static final String SROTEL_PROXY_PASSWORD = "otel.exporter.proxy.password";
    static final String SROTEL_PROXY_HOST = "otel.exporter.proxy.host";
    static final String SROTEL_PROXY_PORT = "otel.exporter.proxy.port";
}
