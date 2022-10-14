package io.smallrye.opentelemetry.test;

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_FLAVOR;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

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
