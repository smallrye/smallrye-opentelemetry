package io.smallrye.opentelemetry.implementation.exporters.logs;

import java.util.Collection;

import io.opentelemetry.exporter.internal.grpc.GrpcExporter;
import io.opentelemetry.exporter.internal.otlp.logs.LogsRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;

public class VertxGrpcLogsExporter implements LogRecordExporter {
    private final GrpcExporter<LogsRequestMarshaler> delegate;

    public VertxGrpcLogsExporter(GrpcExporter<LogsRequestMarshaler> delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableResultCode export(Collection<LogRecordData> logs) {
        return delegate.export(LogsRequestMarshaler.create(logs), logs.size());
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }
}
