package io.smallrye.opentelemetry.implementation.rest.observation.client;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;

import io.micrometer.observation.transport.RequestReplySenderContext;

public class ObservationClientContext extends RequestReplySenderContext<ClientRequestContext, ClientResponseContext> {

    private final ClientRequestContext requestContext;
    private ClientResponseContext responseContext;

    public ObservationClientContext(final ClientRequestContext requestContext) {
        super((carrier, key, value) -> {
            // This is a very naive approach that takes the first ConsumerRecord
            carrier.getHeaders().add(key, value);
        });
        this.requestContext = requestContext;
        setCarrier(requestContext);
    }

    //    // fixme remove getters and setters
    //    public ClientRequestContext getRequestContext() {
    //        return requestContext;
    //    }
    //
    //    public ClientResponseContext getResponseContext() {
    //        return responseContext;
    //    }
    //
    //    public void setResponseContext(ClientResponseContext responseContext) {
    //        this.responseContext = responseContext;
    //    }
}
