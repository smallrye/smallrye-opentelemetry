package io.smallrye.opentelemetry.implementation.rest.observation.server;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;

import io.micrometer.observation.transport.RequestReplyReceiverContext;

public class ObservationServerContext extends RequestReplyReceiverContext<ContainerRequestContext, ContainerResponseContext> {
    private final ContainerRequestContext requestContext;
    private final ResourceInfo resourceInfo;

    public ObservationServerContext(final ContainerRequestContext requestContext, final ResourceInfo resourceInfo) {
        super((carrier, key) -> {
            // This is a very naive approach that takes the first ConsumerRecord
            String headerValue = carrier.getHeaders().getFirst(key);
            if (headerValue != null) {
                return headerValue;
            }
            return null;
        });
        this.requestContext = requestContext;
        setCarrier(requestContext);
        this.resourceInfo = resourceInfo;
    }

    public ResourceInfo getResourceInfo() {
        return resourceInfo;
    }
}
