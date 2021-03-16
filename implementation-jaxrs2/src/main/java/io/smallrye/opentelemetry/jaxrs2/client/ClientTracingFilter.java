package io.smallrye.opentelemetry.jaxrs2.client;

import static io.smallrye.opentelemetry.jaxrs2.server.internal.SpanWrapper.PROPERTY_NAME;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.Tracer;
import io.opentelemetry.trace.TracingContextUtils;
import io.smallrye.opentelemetry.api.Traced;
import io.smallrye.opentelemetry.jaxrs2.server.internal.CastUtils;
import io.smallrye.opentelemetry.jaxrs2.server.internal.SpanWrapper;

/**
 * @author Pavol Loffay, Felix Wong
 */
@Priority(Priorities.HEADER_DECORATOR)
public class ClientTracingFilter implements ClientRequestFilter, ClientResponseFilter {

    private static final Logger log = Logger.getLogger(ClientTracingFilter.class.getName());

    private Tracer tracer;
    private List<ClientSpanDecorator> spanDecorators;

    public ClientTracingFilter(Tracer tracer, List<ClientSpanDecorator> spanDecorators) {
        this.tracer = tracer;
        this.spanDecorators = new ArrayList<>(spanDecorators);
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        if (tracingDisabled(requestContext)) {
            log.finest("Client tracing disabled");
            return;
        }

        // in case filter is registered twice
        if (requestContext.getProperty(PROPERTY_NAME) != null) {
            return;
        }

        SpanContext parentSpanContext = CastUtils.cast(requestContext.getProperty(TracingProperties.CHILD_OF),
                SpanContext.class);

        Span span = null;

        if (parentSpanContext != null) {
            span = tracer.spanBuilder(requestContext.getMethod())
                    .setSpanKind(Kind.CLIENT)
                    .setParent(parentSpanContext)
                    .startSpan();

        } else {
            span = tracer.spanBuilder(requestContext.getMethod())
                    .setSpanKind(Kind.CLIENT)
                    .startSpan();

        }

        if (spanDecorators != null) {
            for (ClientSpanDecorator decorator : spanDecorators) {
                decorator.decorateRequest(requestContext, span);
            }
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Starting client span");
        }

        Context context = TracingContextUtils.withSpan(span, Context.current());
        OpenTelemetry.getPropagators().getTextMapPropagator().inject(context, requestContext.getHeaders(),
                new ClientHeadersInjectTextMap());

        requestContext.setProperty(PROPERTY_NAME, new SpanWrapper(span, null));
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        SpanWrapper spanWrapper = CastUtils
                .cast(requestContext.getProperty(PROPERTY_NAME), SpanWrapper.class);
        if (spanWrapper != null && !spanWrapper.isFinished()) {
            log.finest("Finishing client span");

            if (spanDecorators != null) {
                for (ClientSpanDecorator decorator : spanDecorators) {
                    decorator.decorateResponse(responseContext, spanWrapper.get());
                }
            }

            spanWrapper.finish();
        }
    }

    private boolean tracingDisabled(ClientRequestContext clientRequestContext) {
        Boolean tracingDisabled = CastUtils.cast(clientRequestContext.getProperty(TracingProperties.TRACING_DISABLED),
                Boolean.class);
        if (tracingDisabled != null && tracingDisabled) {
            return true;
        }

        Object invokedMethod = clientRequestContext.getProperty("org.eclipse.microprofile.rest.client.invokedMethod");
        if (invokedMethod == null) {
            return false;
        }

        Method method = (Method) invokedMethod;
        Traced traced = method.getAnnotation(Traced.class);
        if (traced != null && !traced.value()) {
            return true;
        }
        return false;
    }
}
