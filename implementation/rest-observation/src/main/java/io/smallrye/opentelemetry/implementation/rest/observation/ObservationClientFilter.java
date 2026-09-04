package io.smallrye.opentelemetry.implementation.rest.observation;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.ext.Provider;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
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
