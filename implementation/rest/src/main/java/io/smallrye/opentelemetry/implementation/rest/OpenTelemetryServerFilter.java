package io.smallrye.opentelemetry.implementation.rest;

import static io.smallrye.opentelemetry.api.OpenTelemetryConfig.INSTRUMENTATION_NAME;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.lang.reflect.Method;
import java.net.URI;
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
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.smallrye.opentelemetry.api.OpenTelemetryInstrumenter;

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

        this.instrumenter = builder
                .setSpanStatusExtractor(HttpSpanStatusExtractor.create(serverAttributesExtractor))
                .addAttributesExtractor(serverAttributesExtractor)
                .newServerInstrumenter(new ContainerRequestContextTextMapGetter());
    }

    @Override
    public void filter(final ContainerRequestContext request) {
        Context parentContext = Context.current();
        if (instrumenter.shouldStart(parentContext, request)) {
            request.setProperty("rest.resource.class", resourceInfo.getResourceClass());
            request.setProperty("rest.resource.method", resourceInfo.getResourceMethod());

            Context spanContext = instrumenter.start(parentContext, request);
            Scope scope = spanContext.makeCurrent();
            request.setProperty("otel.span.server.context", spanContext);
            request.setProperty("otel.span.server.parentContext", parentContext);
            request.setProperty("otel.span.server.scope", scope);
        }
    }

    @Override
    public void filter(final ContainerRequestContext request, final ContainerResponseContext response) {
        Scope scope = (Scope) request.getProperty("otel.span.server.scope");
        if (scope == null) {
            return;
        }

        Context spanContext = (Context) request.getProperty("otel.span.server.context");
        try {
            instrumenter.end(spanContext, request, response, null);
        } finally {
            scope.close();

            request.removeProperty("rest.resource.class");
            request.removeProperty("rest.resource.method");
            request.removeProperty("otel.span.server.context");
            request.removeProperty("otel.span.server.parentContext");
            request.removeProperty("otel.span.server.scope");
        }
    }

    private static class ContainerRequestContextTextMapGetter implements TextMapGetter<ContainerRequestContext> {
        @Override
        public Iterable<String> keys(final ContainerRequestContext carrier) {
            return carrier.getHeaders().keySet();
        }

        @Override
        public String get(final ContainerRequestContext carrier, final String key) {
            if (carrier == null) {
                return null;
            }

            return carrier.getHeaders().getOrDefault(key, singletonList(null)).get(0);
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
            URI requestUri = request.getUriInfo().getRequestUri();
            String path = requestUri.getPath();
            String query = requestUri.getQuery();
            if (path != null && query != null && !query.isEmpty()) {
                return path + "?" + query;
            }
            return path;
        }

        @Override
        protected String route(final ContainerRequestContext request) {
            Class<?> resourceClass = (Class<?>) request.getProperty("rest.resource.class");
            Method method = (Method) request.getProperty("rest.resource.method");

            UriBuilder template = UriBuilder.fromResource(resourceClass);
            String contextRoot = request.getUriInfo().getBaseUri().getPath();
            if (contextRoot != null) {
                template.path(contextRoot);
            }

            if (method.isAnnotationPresent(Path.class)) {
                template.path(method);
            }

            return template.toTemplate();
        }

        @Override
        protected String scheme(final ContainerRequestContext request) {
            return request.getUriInfo().getRequestUri().getScheme();
        }

        @Override
        protected String serverName(final ContainerRequestContext request, final ContainerResponseContext response) {
            return request.getUriInfo().getRequestUri().getHost();
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
}
