package io.smallrye.opentelemetry.implementation.config.traces.otlp;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.opentelemetry.implementation.config.ConnectionConfig;

import java.util.Optional;

@ConfigMapping(prefix = "otel.exporter.otlp")
public interface OtelExporterConfig extends ConnectionConfig {
    @Override
    String endpoint();

    //    ClientTlsConfig client();  from parent

    //    Map<String, String> headers();  from parent

    //    compression(); from parent

    //    String timeout(); from parent

    //    ExporterType protocol(); from parent
    ExporterOtlpTraceConfig traces();
    // TODO metrics();
    // TODO logs();
}
