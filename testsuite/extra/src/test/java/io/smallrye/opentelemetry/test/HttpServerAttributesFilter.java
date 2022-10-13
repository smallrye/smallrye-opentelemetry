package io.smallrye.opentelemetry.test;

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_FLAVOR;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

@Provider
@PreMatching
public class HttpServerAttributesFilter implements ContainerRequestFilter, ContainerResponseFilter {
    @Context
    HttpServletRequest httpServletRequest;

    @Override
    public void filter(final ContainerRequestContext request) {
        request.setProperty(HTTP_FLAVOR.getKey(), httpServletRequest.getProtocol().split("/")[1]);
    }

    @Override
    public void filter(final ContainerRequestContext request, final ContainerResponseContext response) {

    }
}
