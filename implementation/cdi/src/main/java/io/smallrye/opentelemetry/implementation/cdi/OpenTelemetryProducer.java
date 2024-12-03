package io.smallrye.opentelemetry.implementation.cdi;

import static io.smallrye.opentelemetry.api.OpenTelemetryConfig.INSTRUMENTATION_NAME;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Singleton;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.api.baggage.BaggageEntry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.runtimemetrics.java8.Classes;
import io.opentelemetry.instrumentation.runtimemetrics.java8.Cpu;
import io.opentelemetry.instrumentation.runtimemetrics.java8.GarbageCollector;
import io.opentelemetry.instrumentation.runtimemetrics.java8.MemoryPools;
import io.opentelemetry.instrumentation.runtimemetrics.java8.Threads;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.smallrye.opentelemetry.api.OpenTelemetryBuilderGetter;
import io.smallrye.opentelemetry.api.OpenTelemetryConfig;
import io.smallrye.opentelemetry.api.OpenTelemetryHandler;

@Singleton
public class OpenTelemetryProducer {

    // n.b. while the security manager is supported by this project, take extra caution when evolving this class
    // since otel initialization does require a lot of different file/logging/runtime/management security permissions

    private final List<AutoCloseable> closeables = new ArrayList<>();

    @Produces
    @Singleton
    public OpenTelemetry getOpenTelemetry(final OpenTelemetryConfig config) {
        AutoConfiguredOpenTelemetrySdkBuilder autoConfiguredOpenTelemetrySdkBuilder = new OpenTelemetryBuilderGetter()
                .apply(config).disableShutdownHook();

        AutoConfiguredOpenTelemetrySdk otelSdk;
        if (System.getSecurityManager() == null) {
            otelSdk = autoConfiguredOpenTelemetrySdkBuilder.build();
        } else {
            // Requires FilePermission/RuntimePermission
            otelSdk = AccessController.doPrivileged(
                    (PrivilegedAction<AutoConfiguredOpenTelemetrySdk>) autoConfiguredOpenTelemetrySdkBuilder::build);
        }

        OpenTelemetry openTelemetry = otelSdk.getOpenTelemetrySdk();

        closeables.addAll(Classes.registerObservers(openTelemetry));

        List<AutoCloseable> cpuObservers;
        List<AutoCloseable> garbageCollectorObservers;

        if (System.getSecurityManager() == null) {
            cpuObservers = Cpu.registerObservers(openTelemetry);
            garbageCollectorObservers = GarbageCollector.registerObservers(openTelemetry);
        } else {
            // Requires FilePermission/RuntimePermission/ManagementPermission
            cpuObservers = AccessController.doPrivileged(
                    (PrivilegedAction<List<AutoCloseable>>) () -> Cpu.registerObservers(openTelemetry));
            // Requires FilePermission/RuntimePermission
            garbageCollectorObservers = AccessController.doPrivileged(
                    (PrivilegedAction<List<AutoCloseable>>) () -> GarbageCollector.registerObservers(openTelemetry));
        }

        closeables.addAll(cpuObservers);
        closeables.addAll(garbageCollectorObservers);
        closeables.addAll(MemoryPools.registerObservers(openTelemetry));
        closeables.addAll(Threads.registerObservers(openTelemetry));

        if (System.getSecurityManager() == null) {
            OpenTelemetryHandler.install(openTelemetry);
        } else {
            // Requires LoggingPermission
            AccessController.doPrivileged((PrivilegedAction<List<AutoCloseable>>) () -> {
                OpenTelemetryHandler.install(openTelemetry);
                return null;
            });
        }

        return openTelemetry;
    }

    @Produces
    @Singleton
    public Tracer getTracer() {
        return CDI.current().select(OpenTelemetry.class).get().getTracer(INSTRUMENTATION_NAME);
    }

    @Produces
    @Singleton
    public Meter getMeter() {
        return CDI.current().select(OpenTelemetry.class).get().getMeter(INSTRUMENTATION_NAME);
    }

    @Produces
    @RequestScoped
    @SuppressWarnings("NullableProblems")
    public Span getSpan() {
        return new Span() {
            @Override
            public <T> Span setAttribute(final AttributeKey<T> key, final T value) {
                return Span.current().setAttribute(key, value);
            }

            @Override
            public Span addEvent(final String name, final Attributes attributes) {
                return Span.current().addEvent(name, attributes);
            }

            @Override
            public Span addEvent(final String name, final Attributes attributes, final long timestamp, final TimeUnit unit) {
                return Span.current().addEvent(name, attributes, timestamp, unit);
            }

            @Override
            public Span setStatus(final StatusCode statusCode, final String description) {
                return Span.current().setStatus(statusCode, description);
            }

            @Override
            public Span recordException(final Throwable exception, final Attributes additionalAttributes) {
                return Span.current().recordException(exception, additionalAttributes);
            }

            @Override
            public Span updateName(final String name) {
                return Span.current().updateName(name);
            }

            @Override
            public void end() {
                Span.current().end();
            }

            @Override
            public void end(final long timestamp, final TimeUnit unit) {
                Span.current().end(timestamp, unit);
            }

            @Override
            public SpanContext getSpanContext() {
                return Span.current().getSpanContext();
            }

            @Override
            public boolean isRecording() {
                return Span.current().isRecording();
            }
        };
    }

    @Produces
    @RequestScoped
    @SuppressWarnings("NullableProblems")
    public Baggage getBaggage() {
        return new Baggage() {
            @Override
            public int size() {
                return Baggage.current().size();
            }

            @Override
            public void forEach(final BiConsumer<? super String, ? super BaggageEntry> consumer) {
                Baggage.current().forEach(consumer);
            }

            @Override
            public Map<String, BaggageEntry> asMap() {
                return Baggage.current().asMap();
            }

            @Override
            public String getEntryValue(final String entryKey) {
                return Baggage.current().getEntryValue(entryKey);
            }

            @Override
            public BaggageBuilder toBuilder() {
                return Baggage.current().toBuilder();
            }
        };
    }

    void close(@Disposes final OpenTelemetry openTelemetry) throws Exception {
        for (AutoCloseable closeable : closeables) {
            closeable.close();
        }

        OpenTelemetrySdk openTelemetrySdk = (OpenTelemetrySdk) openTelemetry;
        List<CompletableResultCode> shutdown = new ArrayList<>();
        shutdown.add(openTelemetrySdk.getSdkTracerProvider().shutdown());
        shutdown.add(openTelemetrySdk.getSdkMeterProvider().shutdown());
        shutdown.add(openTelemetrySdk.getSdkLoggerProvider().shutdown());
        CompletableResultCode.ofAll(shutdown).join(10, TimeUnit.SECONDS);
    }
}
