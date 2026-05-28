package io.smallrye.opentelemetry.implementation.exporters.logs;

import static io.smallrye.opentelemetry.implementation.exporters.Constants.PROTOCOL_GRPC;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.PROTOCOL_HTTP_PROTOBUF;
import static io.smallrye.opentelemetry.implementation.exporters.OtlpExporterUtil.getProtocol;

import java.net.URISyntaxException;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.smallrye.opentelemetry.implementation.exporters.AbstractVertxExporterProvider;
import io.smallrye.opentelemetry.senders.VertxGrpcSender;
import io.smallrye.opentelemetry.senders.VertxHttpSender;

public class VertxLogsExporterProvider extends AbstractVertxExporterProvider
        implements ConfigurableLogRecordExporterProvider {
    public VertxLogsExporterProvider() {
        super("log", "otlp");
    }

    @Override
    public LogRecordExporter createExporter(ConfigProperties config) {
        try {
            final String protocol = getProtocol(config, getSignalType());

            if (PROTOCOL_GRPC.equals(protocol)) {
                return new VertxGrpcLogsExporter(createGrpcSender(config, VertxGrpcSender.GRPC_LOG_SERVICE_NAME));
            } else if (PROTOCOL_HTTP_PROTOBUF.equals(protocol)) {
                return new VertxHttpLogsExporter(createHttpSender(config, VertxHttpSender.LOGS_PATH));
            } else {
                throw buildUnsupportedProtocolException(protocol);
            }
        } catch (IllegalArgumentException | URISyntaxException iae) {
            throw new IllegalStateException("Unable to install OTLP Exporter", iae);
        }
    }
}
