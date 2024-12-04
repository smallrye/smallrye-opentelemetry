package io.smallrye.opentelemetry.instrumentation.observation.handler;

import io.micrometer.common.util.internal.logging.InternalLogger;
import io.micrometer.common.util.internal.logging.InternalLoggerFactory;
import io.micrometer.observation.Observation;
import io.micrometer.observation.transport.Propagator;
import io.micrometer.observation.transport.ReceiverContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;

public class PropagatingReceiverTracingObservationHandler<T extends ReceiverContext>
        extends AbstractTracingObservationHandler<T> {

    private static final InternalLogger log = InternalLoggerFactory
            .getInstance(PropagatingReceiverTracingObservationHandler.class);

    private final Tracer tracer;

    private final TextMapPropagator propagator;

    /**
     * Creates a new instance of {@link PropagatingReceiverTracingObservationHandler}.
     *
     * @param tracer the tracer to use to record events
     * @param propagator the mechanism to propagate tracing information from the carrier
     */
    public PropagatingReceiverTracingObservationHandler(Tracer tracer, TextMapPropagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Override
    public void onStart(T context) {
        SpanBuilder extractedSpan = extract(context.getCarrier(),
                (carrier, key) -> context.getGetter().get(carrier, key));

        extractedSpan = remoteSpanBuilder(context.getKind(), context.getRemoteServiceName(),
                context.getRemoteServiceAddress(), extractedSpan);

        getTracingContext(context).setSpan(customizeExtractedSpan(context, extractedSpan).startSpan());
    }

    private <C> SpanBuilder extract(C carrier, Propagator.Getter<C> getter) {
        Context extracted = this.propagator.extract(Context.current(), carrier, new TextMapGetter<C>() {
            @Override
            public Iterable<String> keys(C carrier) {
                return propagator.fields();
            }

            @Override
            public String get(C carrier, String key) {
                return getter.get(carrier, key);
            }
        });
        // extracted.makeCurrent(); // this actually fixes the baggage test
        return tracer.spanBuilder("receiver").setParent(extracted);// FIXME the name needs to be set
    }

    /**
     *
     * @param context context
     * @param builder span builder
     * @return span builder
     */
    public SpanBuilder customizeExtractedSpan(T context, SpanBuilder builder) {
        return builder;
    }

    @Override
    public void onStop(T context) {
        Span span = getRequiredSpan(context);
        tagSpan(context, span);
        customizeReceiverSpan(context, span);
        span.updateName(context.getContextualName() != null ? context.getContextualName() : context.getName());
        span.end();
    }

    /**
     * Allows to customize the receiver span before reporting it.
     *
     * @param context context
     * @param span span to customize
     */
    public void customizeReceiverSpan(T context, Span span) {

    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ReceiverContext;
    }
}
