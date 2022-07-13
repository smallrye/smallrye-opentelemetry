package io.smallrye.opentelemetry.implementation.config.traces.otlp;

import io.smallrye.config.ConfigMapping;
import io.smallrye.opentelemetry.implementation.config.OtelConnectionConfig;

import java.util.Map;

@ConfigMapping(prefix = "otel.exporter.otlp")
public interface ExporterOtlpTraceConfig extends OtelConnectionConfig {

    /**
     * Sets the OTLP endpoint to connect to. If unset, defaults to {@value Constants#DEFAULT_ENDPOINT_URL}.
     */
    @Override
    String endpoint();

//    ClientTlsConfig client();  from parent

//    Map<String, String> headers();  from parent

//    compression(); from parent

//    String timeout(); from parent

//    ExporterType protocol(); from parent

    class Constants {
        public static final String DEFAULT_ENDPOINT_URL = "http://localhost:4317";
    }
}
