package io.smallrye.opentelemetry.implementation.exporters.logs;

import static io.smallrye.opentelemetry.implementation.exporters.Constants.PROTOCOL_GRPC;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.PROTOCOL_HTTP_PROTOBUF;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.common.internal.StandardComponentId;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.smallrye.opentelemetry.implementation.exporters.AbstractVertxExporterProvider;
import io.smallrye.opentelemetry.implementation.exporters.sender.VertxGrpcSender;
import io.smallrye.opentelemetry.implementation.exporters.sender.VertxHttpSender;

public class VertxLogsExporterProvider extends AbstractVertxExporterProvider
        implements ConfigurableLogRecordExporterProvider {
    public VertxLogsExporterProvider() {
        super("log", "otlp");
    }

    @Override
    public LogRecordExporter createExporter(ConfigProperties config) {
        try {
            final String protocol = getConfiguration().getProtocol();

            if (PROTOCOL_GRPC.equals(protocol)) {
                return new VertxGrpcLogsExporter(createGrpcExporter(
                        VertxGrpcSender.GRPC_LOG_SERVICE_NAME,
                        StandardComponentId.ExporterType.OTLP_GRPC_LOG_EXPORTER));
            } else if (PROTOCOL_HTTP_PROTOBUF.equals(protocol)) {
                return new VertxHttpLogsExporter(createHttpExporter(
                        VertxHttpSender.LOGS_PATH,
                        StandardComponentId.ExporterType.OTLP_HTTP_LOG_EXPORTER));
            } else {
                throw buildUnsupportedProtocolException(protocol);
            }
        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException("Unable to install OTLP Exporter", iae);
        }
    }
}
