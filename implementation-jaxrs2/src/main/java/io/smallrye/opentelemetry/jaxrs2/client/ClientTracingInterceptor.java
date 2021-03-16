package io.smallrye.opentelemetry.jaxrs2.client;

import static io.smallrye.opentelemetry.jaxrs2.server.internal.SpanWrapper.PROPERTY_NAME;

import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.ext.InterceptorContext;

import io.opentelemetry.trace.Tracer;
import io.smallrye.opentelemetry.jaxrs2.serialization.InterceptorSpanDecorator;
import io.smallrye.opentelemetry.jaxrs2.serialization.TracingInterceptor;
import io.smallrye.opentelemetry.jaxrs2.server.internal.CastUtils;
import io.smallrye.opentelemetry.jaxrs2.server.internal.SpanWrapper;

@Priority(Priorities.ENTITY_CODER)
public class ClientTracingInterceptor extends TracingInterceptor {

    public ClientTracingInterceptor(Tracer tracer, List<InterceptorSpanDecorator> spanDecorators) {
        super(tracer, spanDecorators);
    }

    @Override
    protected SpanWrapper findSpan(InterceptorContext context) {
        return CastUtils.cast(context.getProperty(PROPERTY_NAME), SpanWrapper.class);
    }
}
