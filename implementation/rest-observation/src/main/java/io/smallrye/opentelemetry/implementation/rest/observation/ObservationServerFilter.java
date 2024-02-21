package io.smallrye.opentelemetry.implementation.rest.observation;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.ext.Provider;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.smallrye.opentelemetry.implementation.rest.observation.server.DefaultServerFilterConvention;
import io.smallrye.opentelemetry.implementation.rest.observation.server.ObservationServerContext;
import io.smallrye.opentelemetry.implementation.rest.observation.server.ServerFilterConvention;

@Provider
public class ObservationServerFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private ObservationRegistry registry;
    private ServerFilterConvention userServerFilterConvention;

    @jakarta.ws.rs.core.Context
    ResourceInfo resourceInfo;

    // RESTEasy requires no-arg constructor for CDI injection: https://issues.redhat.com/browse/RESTEASY-1538
    public ObservationServerFilter() {
    }

    @Inject
    public ObservationServerFilter(ObservationRegistry registry) {
        this.registry = registry;
        this.userServerFilterConvention = null;
    }

    @Override
    public void filter(final ContainerRequestContext request) {
        // CDI is not available in some contexts even if this library is available on the CP
        if (registry == null) {
            return;
        }
        final ObservationServerContext observationRequestContext = new ObservationServerContext(request, resourceInfo);

        Observation observation = FilterDocumentation.SERVER
                .start(this.userServerFilterConvention,
                        new DefaultServerFilterConvention(),
                        () -> observationRequestContext,
                        registry);

        Observation.Scope observationScope = observation.openScope();
        // the observation req context can be obtained from the observation scope
        request.setProperty("otel.span.server.context",
                new ObservationRequestContextAndScope(observationRequestContext, observationScope));
    }

    @Override
    public void filter(final ContainerRequestContext request, final ContainerResponseContext response) {
        ObservationRequestContextAndScope contextAndScope = (ObservationRequestContextAndScope) request
                .getProperty("otel.span.server.context");

        if (contextAndScope == null) {
            return;
        }

        contextAndScope.getObservationRequestContext().setResponse(response);
        Observation.Scope observationScope = contextAndScope.getObservationScope();

        try {
            observationScope.close();
            observationScope.getCurrentObservation().stop();
        } finally {
            request.removeProperty("otel.span.server.context");
        }
    }

    static class ObservationRequestContextAndScope {
        private final ObservationServerContext observationRequestContext;
        private final Observation.Scope observationScope;

        public ObservationRequestContextAndScope(ObservationServerContext observationRequestContext,
                Observation.Scope observationScope) {
            this.observationRequestContext = observationRequestContext;
            this.observationScope = observationScope;
        }

        public ObservationServerContext getObservationRequestContext() {
            return observationRequestContext;
        }

        public Observation.Scope getObservationScope() {
            return observationScope;
        }
    }
}
