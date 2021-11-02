package io.smallrye.opentelemetry.api;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerBuilder;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;

// To ignore the version and find our Tracer, because the version is hardcoded in the Instrumenter constructor.
public final class OpenTelemetryInstrumenter implements OpenTelemetry {
    private final OpenTelemetry openTelemetry;

    public OpenTelemetryInstrumenter(final OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public TracerProvider getTracerProvider() {
        return openTelemetry.getTracerProvider();
    }

    @Override
    public Tracer getTracer(final String instrumentationName) {
        return openTelemetry.getTracer(instrumentationName);
    }

    @Override
    public Tracer getTracer(
            final String instrumentationName,
            final String instrumentationVersion) {
        return openTelemetry.getTracer(instrumentationName);
    }

    @Override
    public TracerBuilder tracerBuilder(final String instrumentationName) {
        return openTelemetry.tracerBuilder(instrumentationName);
    }

    @Override
    public ContextPropagators getPropagators() {
        return openTelemetry.getPropagators();
    }
}
