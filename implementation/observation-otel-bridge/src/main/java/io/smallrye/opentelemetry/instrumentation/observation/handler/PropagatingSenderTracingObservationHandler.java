package io.smallrye.opentelemetry.instrumentation.observation.handler;

import io.micrometer.common.util.internal.logging.InternalLogger;
import io.micrometer.common.util.internal.logging.InternalLoggerFactory;
import io.micrometer.observation.Observation;
import io.micrometer.observation.transport.SenderContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;

public class PropagatingSenderTracingObservationHandler<T extends SenderContext>
        extends AbstractTracingObservationHandler<T> {

    private static final InternalLogger log = InternalLoggerFactory
            .getInstance(PropagatingSenderTracingObservationHandler.class);

    private final Tracer tracer;

    private final TextMapPropagator propagator;

    /**
     * Creates a new instance of {@link PropagatingSenderTracingObservationHandler}.
     *
     * @param tracer the tracer to use to record events
     * @param propagator the mechanism to propagate tracing information into the carrier
     */
    public PropagatingSenderTracingObservationHandler(Tracer tracer, TextMapPropagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Override
    public void onStart(T context) {
        Span childSpan = createSenderSpan(context);
        try (Scope scope = childSpan.makeCurrent()) {
            this.propagator.inject(Context.current(), context.getCarrier(),
                    (carrier, key, value) -> context.getSetter().set(carrier, key, value));
        }
        getTracingContext(context).setSpan(childSpan);
    }

    /**
     * Method to be used to create a sender span.
     *
     * @param context context
     * @return sender span
     */
    public Span createSenderSpan(T context) {
        Span parentSpan = getParentSpan(context);
        SpanBuilder builder = tracer
                .spanBuilder(context.getContextualName() == null ? context.getName() : context.getContextualName());

        if (parentSpan != null) {
            // Make sure parent is in scope
            try (Scope scope = parentSpan.makeCurrent()) {
                builder = builder.setParent(Context.current());
            }
        }

        builder = remoteSpanBuilder(context.getKind(), context.getRemoteServiceName(),
                context.getRemoteServiceAddress(), builder);

        return builder.startSpan();
    }

    @Override
    public void onError(T context) {
        if (context.getError() != null) {
            getRequiredSpan(context).recordException(context.getError());
        }
    }

    @Override
    public void onStop(T context) {
        Span span = getRequiredSpan(context);
        tagSpan(context, span);
        customizeSenderSpan(context, span);
        span.updateName(context.getContextualName() != null ? context.getContextualName() : context.getName());
        span.end();
    }

    /**
     * Allows to customize the receiver span before reporting it.
     *
     * @param context context
     * @param span span to customize
     */
    public void customizeSenderSpan(T context, Span span) {

    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof SenderContext;
    }
}
