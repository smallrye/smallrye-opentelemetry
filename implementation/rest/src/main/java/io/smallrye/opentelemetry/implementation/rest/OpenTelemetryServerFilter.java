package io.smallrye.opentelemetry.implementation.rest;

import static io.smallrye.opentelemetry.api.OpenTelemetryConfig.INSTRUMENTATION_NAME;
import static java.util.Collections.emptyList;

import java.lang.reflect.Method;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.UriBuilder;
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
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;

@Provider
public class OpenTelemetryServerFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private Instrumenter<ContainerRequestContext, ContainerResponseContext> instrumenter;

    @javax.ws.rs.core.Context
    ResourceInfo resourceInfo;

    // RESTEasy requires no-arg constructor for CDI injection: https://issues.redhat.com/browse/RESTEASY-1538
    public OpenTelemetryServerFilter() {
    }

    @Inject
    public OpenTelemetryServerFilter(final OpenTelemetry openTelemetry) {
        ServerAttributesExtractor serverAttributesExtractor = new ServerAttributesExtractor();

        InstrumenterBuilder<ContainerRequestContext, ContainerResponseContext> builder = Instrumenter.newBuilder(
                new OpenTelemetryInstrumenter(openTelemetry),
                INSTRUMENTATION_NAME,
                HttpSpanNameExtractor.create(serverAttributesExtractor));

        this.instrumenter = builder.addAttributesExtractor(serverAttributesExtractor)
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

    private static class ServerAttributesExtractor
            extends HttpServerAttributesExtractor<ContainerRequestContext, ContainerResponseContext> {
        @Override
        protected String flavor(final ContainerRequestContext request) {
            return null;
        }

        @Override
        protected String target(final ContainerRequestContext request) {
            return null;
        }

        @Override
        protected String route(final ContainerRequestContext request) {
            Class<?> resourceClass = (Class<?>) request.getProperty("rest.resource.class");
            Method method = (Method) request.getProperty("rest.resource.method");

            UriBuilder template = UriBuilder.fromResource(resourceClass);
            if (method.isAnnotationPresent(Path.class)) {
                template.path(method);
            }

            return template.toTemplate();
        }

        @Override
        protected String scheme(final ContainerRequestContext request) {
            return null;
        }

        @Override
        protected String serverName(
                final ContainerRequestContext request,
                final ContainerResponseContext response) {
            return null;
        }

        @Override
        protected String method(final ContainerRequestContext request) {
            return request.getMethod();
        }

        @Override
        protected List<String> requestHeader(final ContainerRequestContext request, final String name) {
            return request.getHeaders().getOrDefault(name, emptyList());
        }

        @Override
        protected Long requestContentLength(final ContainerRequestContext request, final ContainerResponseContext response) {
            return null;
        }

        @Override
        protected Long requestContentLengthUncompressed(final ContainerRequestContext request,
                final ContainerResponseContext response) {
            return null;
        }

        @Override
        protected Integer statusCode(final ContainerRequestContext request, final ContainerResponseContext response) {
            return response.getStatus();
        }

        @Override
        protected Long responseContentLength(final ContainerRequestContext request, final ContainerResponseContext response) {
            return null;
        }

        @Override
        protected Long responseContentLengthUncompressed(final ContainerRequestContext request,
                final ContainerResponseContext response) {
            return null;
        }

        @Override
        protected List<String> responseHeader(final ContainerRequestContext request, final ContainerResponseContext response,
                final String name) {
            return response.getStringHeaders().getOrDefault(name, emptyList());
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
