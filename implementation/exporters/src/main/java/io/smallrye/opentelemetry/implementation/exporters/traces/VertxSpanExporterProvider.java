package io.smallrye.opentelemetry.implementation.exporters.traces;

import static io.smallrye.opentelemetry.implementation.exporters.Constants.PROTOCOL_GRPC;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.PROTOCOL_HTTP_PROTOBUF;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.common.internal.StandardComponentId;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.smallrye.opentelemetry.implementation.exporters.AbstractVertxExporterProvider;
import io.smallrye.opentelemetry.implementation.exporters.sender.VertxGrpcSender;
import io.smallrye.opentelemetry.implementation.exporters.sender.VertxHttpSender;

public class VertxSpanExporterProvider extends AbstractVertxExporterProvider
        implements ConfigurableSpanExporterProvider {

    public VertxSpanExporterProvider() {
        super("span", "otlp");
    }

    @Override
    public SpanExporter createExporter(final ConfigProperties config) {
        try {
            final String protocol = getConfiguration().getProtocol();

            if (PROTOCOL_GRPC.equals(protocol)) {
                return new VertxGrpcSpanExporter(createGrpcExporter(
                        VertxGrpcSender.GRPC_TRACE_SERVICE_NAME,
                        StandardComponentId.ExporterType.OTLP_GRPC_SPAN_EXPORTER));
            } else if (PROTOCOL_HTTP_PROTOBUF.equals(protocol)) {
                return new VertxHttpSpanExporter(createHttpExporter(
                        VertxHttpSender.TRACES_PATH,
                        StandardComponentId.ExporterType.OTLP_HTTP_SPAN_EXPORTER));
            } else {
                throw buildUnsupportedProtocolException(protocol);
            }
        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException("Unable to install OTLP Exporter", iae);
        }
    }
}
