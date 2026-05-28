package io.smallrye.opentelemetry.implementation.exporters.logs;

import java.util.Collection;

import io.opentelemetry.exporter.internal.otlp.logs.LogsRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.GrpcSender;
import io.opentelemetry.sdk.common.export.GrpcStatusCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;

public class VertxGrpcLogsExporter implements LogRecordExporter {
    private final GrpcSender sender;

    public VertxGrpcLogsExporter(GrpcSender sender) {
        this.sender = sender;
    }

    @Override
    public CompletableResultCode export(Collection<LogRecordData> logs) {
        LogsRequestMarshaler marshaler = LogsRequestMarshaler.create(logs);
        CompletableResultCode result = new CompletableResultCode();
        sender.send(marshaler.toBinaryMessageWriter(),
                response -> {
                    if (response.getStatusCode() == GrpcStatusCode.OK) {
                        result.succeed();
                    } else {
                        result.fail();
                    }
                },
                throwable -> result.fail());
        return result;
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return sender.shutdown();
    }
}
