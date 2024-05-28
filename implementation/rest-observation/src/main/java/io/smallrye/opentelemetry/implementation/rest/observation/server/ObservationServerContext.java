package io.smallrye.opentelemetry.implementation.rest.observation.server;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;

import io.micrometer.observation.Observation;

public class ObservationServerContext extends Observation.Context {
    private final ContainerRequestContext requestContext;
    private final ResourceInfo resourceInfo;
    private ContainerResponseContext responseContext;

    public ObservationServerContext(final ContainerRequestContext requestContext, final ResourceInfo resourceInfo) {
        this.requestContext = requestContext;
        this.resourceInfo = resourceInfo;
    }

    public ContainerRequestContext getRequestContext() {
        return requestContext;
    }

    public ResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    public ContainerResponseContext getResponseContext() {
        return responseContext;
    }

    public void setResponseContext(ContainerResponseContext responseContext) {
        this.responseContext = responseContext;
    }
}
