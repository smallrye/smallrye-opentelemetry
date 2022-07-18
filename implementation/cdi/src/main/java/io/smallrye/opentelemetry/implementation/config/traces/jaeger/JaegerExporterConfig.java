package io.smallrye.opentelemetry.implementation.config.traces.jaeger;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.opentelemetry.implementation.config.ConnectionConfig;
import io.smallrye.opentelemetry.implementation.config.traces.otlp.ExporterOtlpTraceConfig;

import java.util.Optional;

@ConfigMapping(prefix = "otel.exporter.jaeger")
public interface JaegerExporterConfig extends ConnectionConfig {
    @Override
    String endpoint();

    //    ClientTlsConfig client();  from parent

    //    Map<String, String> headers();  from parent

    //    compression(); from parent

    //    String timeout(); from parent

    //    ExporterType protocol(); from parent
    ExporterJaegerTraceConfig traces();
    // TODO metrics();
    // TODO logs();
}
