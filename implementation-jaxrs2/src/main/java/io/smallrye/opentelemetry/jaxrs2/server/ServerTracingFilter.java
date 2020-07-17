package io.smallrye.opentelemetry.jaxrs2.server;

import static io.smallrye.opentelemetry.jaxrs2.server.internal.SpanWrapper.PROPERTY_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.Tracer;
import io.opentelemetry.trace.TracingContextUtils;
import io.smallrye.opentelemetry.jaxrs2.server.internal.CastUtils;
import io.smallrye.opentelemetry.jaxrs2.server.internal.SpanWrapper;

/**
 * @author Pavol Loffay
 */
@Priority(Priorities.HEADER_DECORATOR)
public class ServerTracingFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger log = Logger.getLogger(ServerTracingFilter.class.getName());

    private static final ServerHeadersExtractTextMap HTTP_GETTER = new ServerHeadersExtractTextMap();

    private final Tracer tracer;
    private final List<ServerSpanDecorator> spanDecorators;
    private final String operationName;
    private final OperationNameProvider operationNameProvider;
    private final Pattern skipPattern;
    private final boolean joinExistingActiveSpan;

    protected ServerTracingFilter(
            Tracer tracer,
            String operationName,
            List<ServerSpanDecorator> spanDecorators,
            OperationNameProvider operationNameProvider,
            Pattern skipPattern,
            boolean joinExistingActiveSpan) {
        this.tracer = tracer;
        this.operationName = operationName;
        this.spanDecorators = new ArrayList<>(spanDecorators);
        this.operationNameProvider = operationNameProvider;
        this.skipPattern = skipPattern;
        this.joinExistingActiveSpan = joinExistingActiveSpan;
    }

    @javax.ws.rs.core.Context
    private HttpServletRequest httpServletRequest;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // return in case filter if registered twice
        if (requestContext.getProperty(PROPERTY_NAME) != null || matchesSkipPattern(requestContext) || tracer == null) {
            return;
        }

        SpanContext parentSpanContext = parentSpanContext(requestContext);
        Span span = tracer.spanBuilder(operationNameProvider.operationName(requestContext))
                .setSpanKind(Kind.SERVER)
                .addLink(parentSpanContext)
                .startSpan();

        if (spanDecorators != null) {
            for (ServerSpanDecorator decorator : spanDecorators) {
                decorator.decorateRequest(requestContext, span);
            }
        }

        // override operation name set by @Traced
        if (this.operationName != null) {
            span.updateName(operationName);
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Creating server span: " + operationName);
        }

        requestContext.setProperty(PROPERTY_NAME, new SpanWrapper(span, tracer.withSpan(span)));
    }

    /**
     * Returns a parent for a span created by this filter (jax-rs span).
     * The context from the active span takes precedence over context in the request,
     * but only if joinExistingActiveSpan has been set.
     * The current active span should be child-of extracted context and for example
     * created at a lower level e.g. jersey filter.
     */
    private SpanContext parentSpanContext(ContainerRequestContext requestContext) {
        Span currentSpan = tracer.getCurrentSpan();
        if (currentSpan != null && this.joinExistingActiveSpan) {
            return currentSpan.getContext();
        }
        Context context = OpenTelemetry.getPropagators()
                .getHttpTextFormat()
                .extract(Context.current(), requestContext.getHeaders(), HTTP_GETTER);
        Span span = TracingContextUtils.getSpan(context);
        return span != null ? span.getContext() : null;
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
            ContainerResponseContext responseContext) {
        SpanWrapper spanWrapper = CastUtils.cast(requestContext.getProperty(PROPERTY_NAME), SpanWrapper.class);
        if (spanWrapper == null) {
            return;
        }

        for (ServerSpanDecorator decorator : spanDecorators) {
            decorator.decorateResponse(responseContext, spanWrapper.get());
        }
    }

    private boolean matchesSkipPattern(ContainerRequestContext requestContext) {
        // skip URLs matching skip pattern
        // e.g. pattern is defined as '/health|/status' then URL 'http://localhost:5000/context/health' won't be traced
        String path = requestContext.getUriInfo().getPath();
        if (skipPattern != null && path != null) {
            if (path.length() > 0 && path.charAt(0) != '/') {
                path = "/" + path;
            }
            return skipPattern.matcher(path).matches();
        }
        return false;
    }
}
