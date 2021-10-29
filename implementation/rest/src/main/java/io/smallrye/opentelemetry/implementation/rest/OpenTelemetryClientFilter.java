package io.smallrye.opentelemetry.implementation.rest;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
public class OpenTelemetryClientFilter implements ClientRequestFilter, ClientResponseFilter {
    @Override
    public void filter(final ClientRequestContext request) {

    }

    @Override
    public void filter(final ClientRequestContext request, final ClientResponseContext response) {

    }
}
