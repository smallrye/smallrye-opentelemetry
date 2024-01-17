package io.smallrye.opentelemetry.instrumentation.observation.handler;

import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.micrometer.common.util.StringUtils;
import io.micrometer.observation.Observation;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.smallrye.opentelemetry.instrumentation.observation.context.TracingObservationContext;

@Singleton
public class OpenTelemetryObservationHandler extends AbstractTracingObservationHandler<Observation.Context> {

    private static final Logger logger = Logger.getLogger(OpenTelemetryObservationHandler.class.getName());

    @Inject
    Tracer tracer;

    @Override
    public void onStart(Observation.Context context) {
        Span parentSpan = getParentSpan(context);
        Span childSpan = parentSpan != null ? nextSpan(tracer, parentSpan) : nextSpan(tracer);
        getTracingContext(context).setSpan(childSpan);
    }

    @Override
    public void onStop(Observation.Context context) {
        TracingObservationContext tracingContext = getTracingContext(context);
        Span span = tracingContext.getSpan();
        span.updateName(getSpanName(context));
        tagSpan(context, span);
        span.end();
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return true;
    }

    private Span nextSpan(Tracer tracer, Span parent) {
        if (parent == null) {
            return nextSpan(tracer);
        }

        return this.tracer.spanBuilder("")
                .setParent(Context.current())
                .startSpan();
    }

    private Span nextSpan(Tracer tracer) {
        return tracer.spanBuilder("").startSpan();
    }

    private String getSpanName(Observation.Context context) {
        String name = context.getName();
        if (StringUtils.isNotBlank(context.getContextualName())) {
            name = context.getContextualName();
        }
        return name;
    }
}
