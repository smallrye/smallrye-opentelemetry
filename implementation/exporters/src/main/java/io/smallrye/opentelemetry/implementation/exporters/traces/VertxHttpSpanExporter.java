package io.smallrye.opentelemetry.implementation.exporters.traces;

import java.util.Collection;

import io.opentelemetry.exporter.internal.otlp.traces.TraceRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.HttpSender;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public final class VertxHttpSpanExporter implements SpanExporter {

    private final HttpSender sender;

    public VertxHttpSpanExporter(HttpSender sender) {
        this.sender = sender;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        TraceRequestMarshaler marshaler = TraceRequestMarshaler.create(spans);
        CompletableResultCode result = new CompletableResultCode();
        sender.send(marshaler.toBinaryMessageWriter(),
                response -> {
                    if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
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
