package io.smallrye.opentelemetry.implementation.rest;

import static io.opentelemetry.semconv.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_STATUS_CODE;
import static io.smallrye.opentelemetry.api.OpenTelemetryConfig.INSTRUMENTATION_NAME;
import static io.smallrye.opentelemetry.api.OpenTelemetryConfig.INSTRUMENTATION_VERSION;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.ext.Provider;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientExperimentalMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.opentelemetry.instrumentation.api.internal.SemconvStability;

@Provider
public class OpenTelemetryClientFilter implements ClientRequestFilter, ClientResponseFilter {
    private Instrumenter<ClientRequestContext, ClientResponseContext> instrumenter;
    private LongHistogram durationHistogram;

    // RESTEasy requires no-arg constructor for CDI injection: https://issues.redhat.com/browse/RESTEASY-1538
    public OpenTelemetryClientFilter() {
    }

    @Inject
    public OpenTelemetryClientFilter(final OpenTelemetry openTelemetry) {
        ClientAttributesExtractor clientAttributesExtractor = new ClientAttributesExtractor();

        // TODO - The Client Span name is only "HTTP {METHOD_NAME}": https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/http.md#name
        final InstrumenterBuilder<ClientRequestContext, ClientResponseContext> builder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                HttpSpanNameExtractor.create(clientAttributesExtractor));
        builder.setInstrumentationVersion(INSTRUMENTATION_VERSION);

        this.instrumenter = builder
                .setSpanStatusExtractor(HttpSpanStatusExtractor.create(clientAttributesExtractor))
                .addAttributesExtractor(HttpClientAttributesExtractor.create(clientAttributesExtractor))
                .addOperationMetrics(HttpClientMetrics.get())
                .addOperationMetrics(HttpClientExperimentalMetrics.get())
                .buildClientInstrumenter(new ClientRequestContextTextMapSetter());

        final Meter meter = openTelemetry.getMeter(INSTRUMENTATION_NAME);
        //fixme use new https://opentelemetry.io/docs/specs/semconv/http/http-metrics/#metric-httpclientrequestduration
        durationHistogram = meter.histogramBuilder("http.client.duration")
                .setDescription("The duration of an outbound HTTP request")
                .ofLongs()
                .setUnit("ms")
                .build();
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
        if (durationHistogram != null) {
            request.setProperty("otel.metrics.client.start", System.currentTimeMillis());
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
        if (durationHistogram != null) {
            Long start = (Long) request.getProperty("otel.metrics.client.start");
            if (start != null) {
                try {
                    durationHistogram.record(System.currentTimeMillis() - start,
                            getHistogramAttributes(request, response),
                            spanContext);
                } finally {
                    request.removeProperty("otel.metrics.client.start");
                }
            }
        }
    }

    private Attributes getHistogramAttributes(ClientRequestContext request, ClientResponseContext response) {
        AttributesBuilder builder = Attributes.builder();
        builder.put(HTTP_ROUTE.getKey(), request.getUri().getPath().toString());// Fixme must contain a template /users/:userID?
        if (SemconvStability.emitOldHttpSemconv()) {
            builder.put(HTTP_METHOD, request.getMethod());// FIXME semantic conventions
            builder.put(HTTP_STATUS_CODE, response.getStatus());
        } else {
            builder.put(HTTP_REQUEST_METHOD, request.getMethod());// FIXME semantic conventions
            builder.put(HTTP_RESPONSE_STATUS_CODE, response.getStatus());
        }
        return builder.build();
    }

    private static class ClientRequestContextTextMapSetter implements TextMapSetter<ClientRequestContext> {
        @Override
        public void set(final ClientRequestContext carrier, final String key, final String value) {
            if (carrier != null) {
                carrier.getHeaders().put(key, singletonList(value));
            }
        }
    }

    private static class ClientAttributesExtractor
            implements HttpClientAttributesGetter<ClientRequestContext, ClientResponseContext> {

        @Override
        public String getUrlFull(final ClientRequestContext request) {
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
