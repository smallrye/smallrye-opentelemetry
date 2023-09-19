package io.smallrye.opentelemetry.implementation.rest;

import static io.smallrye.opentelemetry.api.OpenTelemetryConfig.INSTRUMENTATION_NAME;
import static io.smallrye.opentelemetry.api.OpenTelemetryConfig.INSTRUMENTATION_VERSION;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

@Provider
public class OpenTelemetryServerFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private Instrumenter<ContainerRequestContext, ContainerResponseContext> instrumenter;

    @jakarta.ws.rs.core.Context
    ResourceInfo resourceInfo;

    // RESTEasy requires no-arg constructor for CDI injection: https://issues.redhat.com/browse/RESTEASY-1538
    public OpenTelemetryServerFilter() {
    }

    @Inject
    public OpenTelemetryServerFilter(final OpenTelemetry openTelemetry) {
        HttpServerAttributesExtractor serverAttributesExtractor = new HttpServerAttributesExtractor();

        InstrumenterBuilder<ContainerRequestContext, ContainerResponseContext> builder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                HttpSpanNameExtractor.create(serverAttributesExtractor));
        builder.setInstrumentationVersion(INSTRUMENTATION_VERSION);

        this.instrumenter = builder
                .setSpanStatusExtractor(HttpSpanStatusExtractor.create(serverAttributesExtractor))
                .addAttributesExtractor(io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesExtractor
                        .create(serverAttributesExtractor, new NetServerAttributesGetter()))
                .buildServerInstrumenter(new ContainerRequestContextTextMapGetter());
    }

    @Override
    public void filter(final ContainerRequestContext request) {
        // CDI is not available in some contexts even if this library is available on the CP
        if (instrumenter != null) {
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
    }

    @Override
    public void filter(final ContainerRequestContext request, final ContainerResponseContext response) {
        if (instrumenter != null) {
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

    private static class NetServerAttributesGetter implements
            io.opentelemetry.instrumentation.api.instrumenter.net.NetServerAttributesGetter<ContainerRequestContext, ContainerResponseContext> {
        @Override
        public String getTransport(final ContainerRequestContext request) {
            return SemanticAttributes.NetTransportValues.IP_TCP;
        }

        @Override
        public String getNetworkProtocolName(final ContainerRequestContext request, final ContainerResponseContext response) {
            return (String) request.getProperty(SemanticAttributes.NET_PROTOCOL_NAME.getKey());
        }

        @Override
        public String getNetworkProtocolVersion(final ContainerRequestContext request,
                final ContainerResponseContext response) {
            return (String) request.getProperty(SemanticAttributes.NET_PROTOCOL_VERSION.getKey());
        }

        @Override
        public String getServerAddress(final ContainerRequestContext request) {
            return request.getUriInfo().getRequestUri().getHost();
        }

        @Override
        public Integer getServerPort(final ContainerRequestContext request) {
            URI uri = request.getUriInfo().getRequestUri();
            if (uri.getPort() > 0) {
                return uri.getPort();
            }
            try {
                return uri.toURL().getDefaultPort();
            } catch (MalformedURLException ex) {
                return -1;
            }
        }

        @Override
        public InetSocketAddress getServerInetSocketAddress(final ContainerRequestContext request,
                final ContainerResponseContext response) {
            String serverAddress = getServerAddress(request);
            Integer serverPort = getServerPort(request);
            if (serverAddress != null && serverPort != null) {
                return new InetSocketAddress(serverAddress, serverPort);
            }
            return null;
        }
    }

    private static class HttpServerAttributesExtractor
            implements HttpServerAttributesGetter<ContainerRequestContext, ContainerResponseContext> {

        @Override
        public String getUrlPath(final ContainerRequestContext request) {
            return request.getUriInfo().getRequestUri().getPath();
        }

        @Override
        public String getUrlQuery(final ContainerRequestContext request) {
            return request.getUriInfo().getRequestUri().getQuery();
        }

        @Override
        public String getHttpRoute(final ContainerRequestContext request) {
            try {
                // This can throw an IllegalArgumentException when determining the route for a subresource
                Class<?> resource = (Class<?>) request.getProperty("rest.resource.class");
                Method method = (Method) request.getProperty("rest.resource.method");

                UriBuilder uriBuilder = UriBuilder.newInstance();
                String contextRoot = request.getUriInfo().getBaseUri().getPath();
                if (contextRoot != null) {
                    uriBuilder.path(contextRoot);
                }
                uriBuilder.path(resource);
                if (method.isAnnotationPresent(Path.class)) {
                    uriBuilder.path(method);
                }

                return uriBuilder.toTemplate();
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        @Override
        public String getUrlScheme(final ContainerRequestContext request) {
            return request.getUriInfo().getRequestUri().getScheme();
        }

        @Override
        public String getHttpRequestMethod(final ContainerRequestContext request) {
            return request.getMethod();
        }

        @Override
        public List<String> getHttpRequestHeader(final ContainerRequestContext request, final String name) {
            return request.getHeaders().getOrDefault(name, emptyList());
        }

        @Override
        public Integer getHttpResponseStatusCode(final ContainerRequestContext request, final ContainerResponseContext response,
                final Throwable throwable) {
            return response.getStatus();
        }

        @Override
        public List<String> getHttpResponseHeader(final ContainerRequestContext request,
                final ContainerResponseContext response,
                final String name) {
            return response.getStringHeaders().getOrDefault(name, emptyList());
        }
    }
}
