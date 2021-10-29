package io.smallrye.opentelemetry.implementation.rest;

import static io.smallrye.opentelemetry.api.OpenTelemetryConfig.INSTRUMENTATION_NAME;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.ext.Provider;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerBuilder;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;

@Provider
public class OpenTelemetryRequestResponseFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private Instrumenter<ContainerRequestContext, ContainerResponseContext> instrumenter;

    @javax.ws.rs.core.Context
    ResourceInfo resourceInfo;

    // RESTEasy requires no-arg constructor for CDI injection: https://issues.redhat.com/browse/RESTEASY-1538
    public OpenTelemetryRequestResponseFilter() {
    }

    @Inject
    public OpenTelemetryRequestResponseFilter(final OpenTelemetry openTelemetry) {
        RestHttpServerAttributesExtractor restHttpServerAttributesExtractor = new RestHttpServerAttributesExtractor();

        InstrumenterBuilder<ContainerRequestContext, ContainerResponseContext> builder = Instrumenter.newBuilder(
                new OpenTelemetryInstrumenter(openTelemetry),
                INSTRUMENTATION_NAME,
                HttpSpanNameExtractor.create(restHttpServerAttributesExtractor));

        this.instrumenter = builder.addAttributesExtractor(restHttpServerAttributesExtractor)
                .newServerInstrumenter(
                        new TextMapGetter<ContainerRequestContext>() {
                            @Override
                            public Iterable<String> keys(final ContainerRequestContext carrier) {
                                return null;
                            }

                            @Override
                            public String get(final ContainerRequestContext carrier, final String key) {
                                return null;
                            }
                        });
    }

    @Override
    public void filter(final ContainerRequestContext request) {
        Context parentContext = Context.current();
        if (instrumenter.shouldStart(parentContext, request)) {
            request.setProperty("rest.resource.class", resourceInfo.getResourceClass());
            request.setProperty("rest.resource.method", resourceInfo.getResourceMethod());

            Context spanContext = instrumenter.start(parentContext, request);
            Scope scope = spanContext.makeCurrent();
            request.setProperty("otel.span.context", spanContext);
            request.setProperty("otel.span.parentContext", parentContext);
            request.setProperty("otel.span.scope", scope);
        }
    }

    @Override
    public void filter(final ContainerRequestContext request, final ContainerResponseContext response) {
        Scope scope = (Scope) request.getProperty("otel.span.scope");
        if (scope == null) {
            return;
        }

        Context spanContext = (Context) request.getProperty("otel.span.context");
        try {
            instrumenter.end(spanContext, request, response, null);
        } finally {
            scope.close();

            request.removeProperty("rest.resource.class");
            request.removeProperty("rest.resource.method");
            request.removeProperty("otel.span.context");
            request.removeProperty("otel.span.parentContext");
            request.removeProperty("otel.span.scope");
        }
    }

    // To ignore the version and find our Tracer, because the version is hardcoded in the Instrumenter constructor.
    private static final class OpenTelemetryInstrumenter implements OpenTelemetry {
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
}
