package io.smallrye.opentelemetry.implementation.rest.observation.client;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;

import io.micrometer.observation.Observation;

public class ObservationClientContext extends Observation.Context {

    private final ClientRequestContext requestContext;
    private ClientResponseContext responseContext;

    public ObservationClientContext(final ClientRequestContext requestContext) {
        this.requestContext = requestContext;
    }

    public ClientRequestContext getRequestContext() {
        return requestContext;
    }

    public ClientResponseContext getResponseContext() {
        return responseContext;
    }

    public void setResponseContext(ClientResponseContext responseContext) {
        this.responseContext = responseContext;
    }
}
