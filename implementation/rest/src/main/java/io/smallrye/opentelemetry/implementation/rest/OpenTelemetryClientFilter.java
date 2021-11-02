package io.smallrye.opentelemetry.implementation.rest;

import static io.smallrye.opentelemetry.api.OpenTelemetryConfig.INSTRUMENTATION_NAME;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.smallrye.opentelemetry.api.OpenTelemetryInstrumenter;

@Provider
public class OpenTelemetryClientFilter implements ClientRequestFilter, ClientResponseFilter {
    private Instrumenter<ClientRequestContext, ClientResponseContext> instrumenter;

    // RESTEasy requires no-arg constructor for CDI injection: https://issues.redhat.com/browse/RESTEASY-1538
    public OpenTelemetryClientFilter() {
    }

    @Inject
    public OpenTelemetryClientFilter(final OpenTelemetry openTelemetry) {
        ClientAttributesExtractor clientAttributesExtractor = new ClientAttributesExtractor();

        InstrumenterBuilder<ClientRequestContext, ClientResponseContext> builder = Instrumenter.newBuilder(
                new OpenTelemetryInstrumenter(openTelemetry),
                INSTRUMENTATION_NAME,
                HttpSpanNameExtractor.create(clientAttributesExtractor));

        this.instrumenter = builder.addAttributesExtractor(clientAttributesExtractor)
                .newClientInstrumenter(new TextMapSetter<ClientRequestContext>() {
                    @Override
                    public void set(
                            final ClientRequestContext carrier,
                            final String key,
                            final String value) {
                        if (carrier != null) {
                            carrier.getHeaders().put(key, singletonList(value));
                        }
                    }
                });
    }

    @Override
    public void filter(final ClientRequestContext request) {
        Context parentContext = Context.current();
        if (instrumenter.shouldStart(parentContext, request)) {
            Context spanContext = instrumenter.start(parentContext, request);
            Scope scope = spanContext.makeCurrent();
            request.setProperty("otel.span.client.context", spanContext);
            request.setProperty("otel.span.client.parentContext", parentContext);
            request.setProperty("otel.span.client.scope", scope);
        }
    }

    @Override
    public void filter(final ClientRequestContext request, final ClientResponseContext response) {
        Scope scope = (Scope) request.getProperty("otel.span.client.scope");
        if (scope == null) {
            return;
        }

        Context spanContext = (Context) request.getProperty("otel.span.client.context");
        try {
            instrumenter.end(spanContext, request, response, null);
        } finally {
            scope.close();

            request.removeProperty("otel.span.client.context");
            request.removeProperty("otel.span.client.parentContext");
            request.removeProperty("otel.span.client.scope");
        }
    }

    private static class ClientAttributesExtractor
            extends HttpClientAttributesExtractor<ClientRequestContext, ClientResponseContext> {

        @Override
        protected String url(final ClientRequestContext request) {
            return null;
        }

        @Override
        protected String flavor(final ClientRequestContext request, final ClientResponseContext response) {
            return null;
        }

        @Override
        protected String method(final ClientRequestContext request) {
            return null;
        }

        @Override
        protected List<String> requestHeader(final ClientRequestContext request, final String name) {
            return request.getStringHeaders().getOrDefault(name, emptyList());
        }

        @Override
        protected Long requestContentLength(final ClientRequestContext request, final ClientResponseContext response) {
            return null;
        }

        @Override
        protected Long requestContentLengthUncompressed(final ClientRequestContext request,
                final ClientResponseContext response) {
            return null;
        }

        @Override
        protected Integer statusCode(final ClientRequestContext request, final ClientResponseContext response) {
            return null;
        }

        @Override
        protected Long responseContentLength(final ClientRequestContext request, final ClientResponseContext response) {
            return null;
        }

        @Override
        protected Long responseContentLengthUncompressed(final ClientRequestContext request,
                final ClientResponseContext response) {
            return null;
        }

        @Override
        protected List<String> responseHeader(final ClientRequestContext request, final ClientResponseContext response,
                final String name) {
            return response.getHeaders().getOrDefault(name, emptyList());
        }
    }
}
