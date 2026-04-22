package io.smallrye.opentelemetry.implementation.exporters.traces;

import java.util.Collection;

import io.opentelemetry.exporter.internal.otlp.traces.TraceRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.GrpcSender;
import io.opentelemetry.sdk.common.export.GrpcStatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public final class VertxGrpcSpanExporter implements SpanExporter {

    private final GrpcSender sender;

    public VertxGrpcSpanExporter(GrpcSender sender) {
        this.sender = sender;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        TraceRequestMarshaler marshaler = TraceRequestMarshaler.create(spans);
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
