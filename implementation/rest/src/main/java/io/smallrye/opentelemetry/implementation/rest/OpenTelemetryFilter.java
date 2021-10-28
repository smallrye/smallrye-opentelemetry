package io.smallrye.opentelemetry.implementation.rest;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import io.opentelemetry.api.trace.Tracer;

@Provider
public class OpenTelemetryFilter implements ContainerRequestFilter, ContainerResponseFilter {
    @Inject
    Tracer tracer;

    @Override
    public void filter(final ContainerRequestContext containerRequestContext) throws IOException {

    }

    @Override
    public void filter(final ContainerRequestContext containerRequestContext,
            final ContainerResponseContext containerResponseContext) throws IOException {

    }
}
