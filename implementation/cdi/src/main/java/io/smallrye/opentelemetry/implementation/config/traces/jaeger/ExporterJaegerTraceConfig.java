package io.smallrye.opentelemetry.implementation.config.traces.jaeger;

import io.smallrye.config.ConfigMapping;
import io.smallrye.opentelemetry.implementation.config.ConnectionConfig;

@ConfigMapping(prefix = "otel.exporter.jaeger")
public interface ExporterJaegerTraceConfig extends ConnectionConfig {
    // TODO default Jaeger endpoint
    @Override
    String endpoint();

    //    ClientTlsConfig client();  from parent

    //    Map<String, String> headers();  from parent

    //    compression(); from parent

    //    String timeout(); from parent

    //    ExporterType protocol(); from parent
}
