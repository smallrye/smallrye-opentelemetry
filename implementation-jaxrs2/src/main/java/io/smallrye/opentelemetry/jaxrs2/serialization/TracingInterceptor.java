package io.smallrye.opentelemetry.jaxrs2.serialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.InterceptorContext;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import io.opentelemetry.context.Scope;
import io.opentelemetry.trace.DefaultSpan;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Span.Builder;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.Tracer;
import io.smallrye.opentelemetry.jaxrs2.server.internal.SpanWrapper;

public abstract class TracingInterceptor implements WriterInterceptor, ReaderInterceptor {
    private static final String SERIALIZE_OPERATION_NAME = "serialize";
    private static final String DESERIALIZE_OPERATION_NAME = "deserialize";

    private final Tracer tracer;
    private final Collection<InterceptorSpanDecorator> spanDecorators;

    public TracingInterceptor(Tracer tracer,
            List<InterceptorSpanDecorator> spanDecorators) {
        Objects.requireNonNull(tracer);
        Objects.requireNonNull(spanDecorators);
        this.tracer = tracer;
        this.spanDecorators = new ArrayList<>(spanDecorators);
    }

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context)
            throws IOException, WebApplicationException {
        Span span = buildSpan(context, DESERIALIZE_OPERATION_NAME);
        try (Scope scope = tracer.withSpan(span)) {
            decorateRead(context, span);
            try {
                return context.proceed();
            } catch (Exception e) {
                //TODO add exception logs in case they are not added by the filter.
                span.setStatus(Status.UNKNOWN);
                throw e;
            }
        } finally {
            span.end();
        }
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context)
            throws IOException, WebApplicationException {
        Span span = buildSpan(context, SERIALIZE_OPERATION_NAME);
        try (Scope scope = tracer.withSpan(span)) {
            decorateWrite(context, span);
            context.proceed();
        } catch (Exception e) {
            span.setStatus(Status.UNKNOWN);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * @param context Used to retrieve the current span wrapper created by the jax-rs request filter.
     * @param operationName "serialize" or "deserialize" depending on the context
     * @return a noop span is no span context is registered in the context. Otherwise a new span related to the current on
     *         retrieved from the context.
     */
    private Span buildSpan(InterceptorContext context, String operationName) {
        final SpanWrapper spanWrapper = findSpan(context);
        if (spanWrapper == null) {
            return DefaultSpan.getInvalid();
        }
        Builder spanBuilder = tracer.spanBuilder(operationName)
                .setNoParent();
        if (spanWrapper.isFinished()) {
            spanBuilder.addLink(spanWrapper.get().getContext());
        } else {
            spanBuilder.setParent(spanWrapper.get());
        }
        return spanBuilder.startSpan();
    }

    protected abstract SpanWrapper findSpan(InterceptorContext context);

    private void decorateRead(InterceptorContext context, Span span) {
        for (InterceptorSpanDecorator decorator : spanDecorators) {
            decorator.decorateRead(context, span);
        }
    }

    private void decorateWrite(InterceptorContext context, Span span) {
        for (InterceptorSpanDecorator decorator : spanDecorators) {
            decorator.decorateWrite(context, span);
        }
    }
}
