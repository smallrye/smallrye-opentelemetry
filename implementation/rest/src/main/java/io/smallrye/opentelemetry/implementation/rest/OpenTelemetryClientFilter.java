package io.smallrye.opentelemetry.implementation.rest;

import static io.smallrye.opentelemetry.api.OpenTelemetryConfig.INSTRUMENTATION_NAME;
import static io.smallrye.opentelemetry.api.OpenTelemetryConfig.INSTRUMENTATION_VERSION;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.ext.Provider;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.http.HttpClientExperimentalMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesGetter;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientMetrics;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanStatusExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.NetworkAttributesExtractor;
import io.opentelemetry.semconv.NetworkAttributes;

@Provider
public class OpenTelemetryClientFilter implements ClientRequestFilter, ClientResponseFilter {
    private Instrumenter<ClientRequestContext, ClientResponseContext> instrumenter;

    // RESTEasy requires no-arg constructor for CDI injection: https://issues.redhat.com/browse/RESTEASY-1538
    public OpenTelemetryClientFilter() {
    }

    @Inject
    public OpenTelemetryClientFilter(final OpenTelemetry openTelemetry) {
        ClientAttributesExtractor clientAttributesExtractor = new ClientAttributesExtractor();

        final InstrumenterBuilder<ClientRequestContext, ClientResponseContext> builder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                HttpSpanNameExtractor.create(clientAttributesExtractor));
        builder.setInstrumentationVersion(INSTRUMENTATION_VERSION);

        this.instrumenter = builder
                .setSpanStatusExtractor(HttpSpanStatusExtractor.create(clientAttributesExtractor))
                .addAttributesExtractor(NetworkAttributesExtractor.create(new NetworkAttributesGetter()))
                .addAttributesExtractor(HttpClientAttributesExtractor.create(clientAttributesExtractor))
                .addOperationMetrics(HttpClientMetrics.get()) // includes histogram from bellow
                .addOperationMetrics(HttpClientExperimentalMetrics.get())
                .buildClientInstrumenter(new ClientRequestContextTextMapSetter());
    }

    @Override
    public void filter(final ClientRequestContext request) {
        // CDI is not available in some contexts even if this library is available on the CP
        if (instrumenter != null) {
            Context parentContext = Context.current();
            if (instrumenter.shouldStart(parentContext, request)) {
                Context spanContext = instrumenter.start(parentContext, request);
                Scope scope = spanContext.makeCurrent();
                request.setProperty("otel.span.client.context", spanContext);
                request.setProperty("otel.span.client.parentContext", parentContext);
                request.setProperty("otel.span.client.scope", scope);
            }
        }
    }

    @Override
    public void filter(final ClientRequestContext request, final ClientResponseContext response) {
        // CDI is not available in some contexts even if this library is available on the CP
        Context spanContext = (Context) request.getProperty("otel.span.client.context");
        if (instrumenter != null) {
            Scope scope = (Scope) request.getProperty("otel.span.client.scope");
            if (scope == null) {
                return;
            }

            try {
                instrumenter.end(spanContext, request, response, null);
            } finally {
                scope.close();

                request.removeProperty("otel.span.client.context");
                request.removeProperty("otel.span.client.parentContext");
                request.removeProperty("otel.span.client.scope");
            }
        }
    }

    @SuppressWarnings("NullableProblems")
    private static class ClientRequestContextTextMapSetter implements TextMapSetter<ClientRequestContext> {
        @Override
        public void set(final ClientRequestContext carrier, final String key, final String value) {
            if (carrier != null) {
                carrier.getHeaders().put(key, singletonList(value));
            }
        }
    }

    private static class NetworkAttributesGetter implements
            io.opentelemetry.instrumentation.api.semconv.network.NetworkAttributesGetter<ClientRequestContext, ClientResponseContext> {
        @Override
        public String getNetworkTransport(final ClientRequestContext request, final ClientResponseContext response) {
            return NetworkAttributes.NetworkTransportValues.TCP;
        }

        @Override
        public String getNetworkProtocolName(final ClientRequestContext request, final ClientResponseContext response) {
            return null;
        }

        @Override
        public String getNetworkProtocolVersion(final ClientRequestContext request, final ClientResponseContext response) {
            return null;
        }

        @Override
        public InetSocketAddress getNetworkPeerInetSocketAddress(final ClientRequestContext request,
                final ClientResponseContext response) {
            URI uri = request.getUri();
            String serverAddress = uri.getHost();
            Integer serverPort = uri.getPort() > 0 ? uri.getPort() : null;
            if (serverAddress != null && serverPort != null) {
                return new InetSocketAddress(serverAddress, serverPort);
            }
            return null;
        }
    }

    @SuppressWarnings("NullableProblems")
    private static class ClientAttributesExtractor
            implements HttpClientAttributesGetter<ClientRequestContext, ClientResponseContext> {

        @Override
        public String getUrlFull(final ClientRequestContext request) {
            // TODO - Make sure this does not contain authentication information
            return request.getUri().toString();
        }

        @Override
        public String getServerAddress(final ClientRequestContext request) {
            return request.getUri().getHost();
        }

        @Override
        public Integer getServerPort(final ClientRequestContext request) {
            return request.getUri().getPort();
        }

        @Override
        public String getHttpRequestMethod(final ClientRequestContext request) {
            return request.getMethod();
        }

        @Override
        public List<String> getHttpRequestHeader(final ClientRequestContext request, final String name) {
            return request.getStringHeaders().getOrDefault(name, emptyList());
        }

        @Override
        public Integer getHttpResponseStatusCode(final ClientRequestContext request, final ClientResponseContext response,
                final Throwable throwable) {
            return response.getStatus();
        }

        @Override
        public List<String> getHttpResponseHeader(final ClientRequestContext request, final ClientResponseContext response,
                final String name) {
            return response.getHeaders().getOrDefault(name, emptyList());
        }
    }
}
