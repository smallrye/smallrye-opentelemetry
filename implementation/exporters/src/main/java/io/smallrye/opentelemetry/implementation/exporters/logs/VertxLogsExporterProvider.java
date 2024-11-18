package io.smallrye.opentelemetry.implementation.exporters.logs;

import static io.smallrye.opentelemetry.implementation.exporters.Constants.PROTOCOL_GRPC;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.PROTOCOL_HTTP_PROTOBUF;
import static io.smallrye.opentelemetry.implementation.exporters.OtlpExporterUtil.getProtocol;

import java.net.URISyntaxException;

import io.opentelemetry.exporter.internal.otlp.logs.LogsRequestMarshaler;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.smallrye.opentelemetry.implementation.exporters.AbstractVertxExporterProvider;
import io.smallrye.opentelemetry.implementation.exporters.sender.VertxGrpcSender;
import io.smallrye.opentelemetry.implementation.exporters.sender.VertxHttpSender;

public class VertxLogsExporterProvider extends AbstractVertxExporterProvider<LogsRequestMarshaler>
        implements ConfigurableLogRecordExporterProvider {
    public VertxLogsExporterProvider() {
        super("logs", "otlp");
    }

    @Override
    public LogRecordExporter createExporter(ConfigProperties config) {
        try {
            final String protocol = getProtocol(config, getSignalType());

            if (PROTOCOL_GRPC.equals(protocol)) {
                return new VertxGrpcLogsExporter(createGrpcExporter(config, VertxGrpcSender.GRPC_TRACE_SERVICE_NAME));
            } else if (PROTOCOL_HTTP_PROTOBUF.equals(protocol)) {
                return new VertxHttpLogsExporter(createHttpExporter(config, VertxHttpSender.LOGS_PATH));
            } else {
                throw buildUnsupportedProtocolException(protocol);
            }
        } catch (IllegalArgumentException | URISyntaxException iae) {
            throw new IllegalStateException("Unable to install OTLP Exporter", iae);
        }
    }
}
