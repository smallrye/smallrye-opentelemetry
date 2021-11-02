package io.smallrye.opentelemetry.implementation.cdi;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class InMemorySpanExporter implements SpanExporter {
    static final InMemorySpanExporterHolder HOLDER = new InMemorySpanExporterHolder();

    private final List<SpanData> finishedSpanItems = new ArrayList<>();
    private boolean isStopped = false;

    public List<SpanData> getFinishedSpanItems() {
        flushAll();
        synchronized (this) {
            return Collections.unmodifiableList(new ArrayList<>(finishedSpanItems));
        }
    }

    public void reset() {
        synchronized (this) {
            finishedSpanItems.clear();
        }
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        synchronized (this) {
            if (isStopped) {
                return CompletableResultCode.ofFailure();
            }
            finishedSpanItems.addAll(spans);
        }
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        synchronized (this) {
            finishedSpanItems.clear();
            isStopped = true;
        }
        return CompletableResultCode.ofSuccess();
    }

    private static void flushAll() {
        try {
            TracerProvider tracerProvider = GlobalOpenTelemetry.get().getTracerProvider();
            Method unobfuscate = tracerProvider.getClass().getMethod("unobfuscate");
            unobfuscate.setAccessible(true);
            SdkTracerProvider sdkTracerProvider = (SdkTracerProvider) unobfuscate.invoke(tracerProvider);
            CompletableResultCode resultCode = sdkTracerProvider.forceFlush();
            while (!resultCode.isDone()) {
                resultCode.join(10, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static final class InMemorySpanExporterHolder implements Supplier<InMemorySpanExporter> {
        private static final Object NO_INIT = new Object();
        private volatile Object instance = NO_INIT;

        @Override
        public InMemorySpanExporter get() {
            Object result = instance;

            if (result == NO_INIT) {
                synchronized (this) {
                    result = instance;
                    if (result == NO_INIT) {
                        instance = result = new InMemorySpanExporter();
                    }
                }
            }

            return (InMemorySpanExporter) result;
        }
    }
}
