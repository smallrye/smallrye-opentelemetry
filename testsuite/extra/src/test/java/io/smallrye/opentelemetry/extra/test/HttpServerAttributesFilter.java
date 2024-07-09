package io.smallrye.opentelemetry.extra.test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

import io.opentelemetry.semconv.NetworkAttributes;

@Provider
@PreMatching
public class HttpServerAttributesFilter implements ContainerRequestFilter, ContainerResponseFilter {
    @Context
    HttpServletRequest httpServletRequest;

    @Override
    public void filter(final ContainerRequestContext request) {
        String[] nameAndVersion = httpServletRequest.getProtocol().split("/");
        request.setProperty(NetworkAttributes.NETWORK_PROTOCOL_NAME.getKey(), nameAndVersion[0]);
        request.setProperty(NetworkAttributes.NETWORK_PROTOCOL_VERSION.getKey(), nameAndVersion[1]);
    }

    @Override
    public void filter(final ContainerRequestContext request, final ContainerResponseContext response) {

    }
}
