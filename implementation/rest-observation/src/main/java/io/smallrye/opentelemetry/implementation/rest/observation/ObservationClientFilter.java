package io.smallrye.opentelemetry.implementation.rest.observation;

import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.ext.Provider;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesGetter;
import io.smallrye.opentelemetry.implementation.rest.observation.client.ClientFilterConvention;
import io.smallrye.opentelemetry.implementation.rest.observation.client.DefaultClientFilterConvention;
import io.smallrye.opentelemetry.implementation.rest.observation.client.ObservationClientContext;

@Provider
public class ObservationClientFilter implements ClientRequestFilter, ClientResponseFilter {
    private ObservationRegistry registry;
    private ClientFilterConvention userClientFilterConvention;

    // RESTEasy requires no-arg constructor for CDI injection: https://issues.redhat.com/browse/RESTEASY-1538
    public ObservationClientFilter() {
    }

    @Inject
    public ObservationClientFilter(ObservationRegistry registry) {
        this.registry = registry;
        this.userClientFilterConvention = null;
    }

    @Override
    public void filter(final ClientRequestContext request) {
        // CDI is not available in some contexts even if this library is available on the CP
        if (registry == null) {
            return;
        }
        final ObservationClientContext observationRequestContext = new ObservationClientContext(request);

        Observation observation = FilterDocumentation.CLIENT
                .start(this.userClientFilterConvention,
                        new DefaultClientFilterConvention(),
                        () -> observationRequestContext,
                        registry);

        Observation.Scope observationScope = observation.openScope();
        request.setProperty("otel.span.client.context",
                new ObservationRequestContextAndScope(observationRequestContext, observationScope));
    }

    @Override
    public void filter(final ClientRequestContext request, final ClientResponseContext response) {
        ObservationRequestContextAndScope contextAndScope = (ObservationRequestContextAndScope) request
                .getProperty("otel.span.client.context");

        if (contextAndScope == null) {
            return;
        }

        contextAndScope.getObservationRequestContext().setResponse(response);
        Observation.Scope observationScope = contextAndScope.getObservationScope();

        try {
            observationScope.close();
            observationScope.getCurrentObservation().stop();
        } finally {
            request.removeProperty("otel.span.client.context");
        }
    }

    private Attributes getHistogramAttributes(ClientRequestContext request, ClientResponseContext response) {
        AttributesBuilder builder = Attributes.builder();
        builder.put(HTTP_ROUTE.getKey(), request.getUri().getPath().toString());// Fixme must contain a template /users/:userID?

        builder.put(HTTP_REQUEST_METHOD, request.getMethod());// FIXME semantic conventions
        builder.put(HTTP_RESPONSE_STATUS_CODE, response.getStatus());

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

    static class ObservationRequestContextAndScope {
        private final ObservationClientContext observationRequestContext;
        private final Observation.Scope observationScope;

        public ObservationRequestContextAndScope(ObservationClientContext observationRequestContext,
                Observation.Scope observationScope) {
            this.observationRequestContext = observationRequestContext;
            this.observationScope = observationScope;
        }

        public ObservationClientContext getObservationRequestContext() {
            return observationRequestContext;
        }

        public Observation.Scope getObservationScope() {
            return observationScope;
        }
    }
}
