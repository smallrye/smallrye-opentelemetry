package io.smallrye.opentelemetry.implementation.exporters.traces;

import static io.smallrye.opentelemetry.implementation.exporters.Constants.PROTOCOL_GRPC;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.PROTOCOL_HTTP_PROTOBUF;
import static io.smallrye.opentelemetry.implementation.exporters.OtlpExporterUtil.getProtocol;

import java.net.URISyntaxException;

import io.opentelemetry.exporter.internal.otlp.traces.TraceRequestMarshaler;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.smallrye.opentelemetry.implementation.exporters.AbstractVertxExporterProvider;
import io.smallrye.opentelemetry.implementation.exporters.sender.VertxGrpcSender;
import io.smallrye.opentelemetry.implementation.exporters.sender.VertxHttpSender;

public class VertxSpanExporterProvider extends AbstractVertxExporterProvider<TraceRequestMarshaler>
        implements ConfigurableSpanExporterProvider {

    public VertxSpanExporterProvider() {
        super("span", "otlp");
    }

    @Override
    public SpanExporter createExporter(final ConfigProperties config) {
        try {
            final String protocol = getProtocol(config, getSignalType());

            if (PROTOCOL_GRPC.equals(protocol)) {
                return new VertxGrpcSpanExporter(createGrpcExporter(config, VertxGrpcSender.GRPC_TRACE_SERVICE_NAME));
            } else if (PROTOCOL_HTTP_PROTOBUF.equals(protocol)) {
                return new VertxHttpSpanExporter(createHttpExporter(config, VertxHttpSender.TRACES_PATH));
            } else {
                throw buildUnsupportedProtocolException(protocol);
            }
        } catch (IllegalArgumentException | URISyntaxException iae) {
            throw new IllegalStateException("Unable to install OTLP Exporter", iae);
        }
    }
}
