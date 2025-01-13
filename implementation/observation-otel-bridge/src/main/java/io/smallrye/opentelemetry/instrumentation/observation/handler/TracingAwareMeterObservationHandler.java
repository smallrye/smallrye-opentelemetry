package io.smallrye.opentelemetry.instrumentation.observation.handler;

import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.Observation;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.smallrye.opentelemetry.instrumentation.observation.context.TracingObservationContext;

public class TracingAwareMeterObservationHandler<T extends Observation.Context> implements MeterObservationHandler<T> {

    private final MeterObservationHandler<T> delegate;

    private final Tracer tracer;

    /**
     * Creates a new instance of {@link TracingAwareMeterObservationHandler}.
     *
     * @param delegate a {@link MeterObservationHandler} delegate
     * @param tracer tracer
     */
    public TracingAwareMeterObservationHandler(MeterObservationHandler<T> delegate, Tracer tracer) {
        this.delegate = delegate;
        this.tracer = tracer;
    }

    @Override
    public void onStart(T context) {
        this.delegate.onStart(context);
    }

    @Override
    public void onError(T context) {
        this.delegate.onError(context);
    }

    @Override
    public void onEvent(Observation.Event event, T context) {
        this.delegate.onEvent(event, context);
    }

    @Override
    public void onScopeOpened(T context) {
        this.delegate.onScopeOpened(context);
    }

    @Override
    public void onScopeClosed(T context) {
        this.delegate.onScopeClosed(context);
    }

    @Override
    public void onStop(T context) {
        TracingObservationContext tracingContext = context
                .getRequired(TracingObservationContext.class);
        Span currentSpan = tracingContext.getSpan();
        if (currentSpan != null) {
            try (Scope scope = currentSpan.makeCurrent()) {
                this.delegate.onStop(context);
            }
        } else {
            this.delegate.onStop(context);
        }
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return this.delegate.supportsContext(context);
    }

}