package io.smallrye.opentelemetry.jaxrs2.server;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;

import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.attributes.SemanticAttributes;
import io.smallrye.opentelemetry.jaxrs2.server.internal.URIUtils;

/**
 * @author Pavol Loffay
 */
public interface ServerSpanDecorator {

    /**
     * Decorate span by incoming object.
     *
     * @param requestContext
     * @param span
     */
    void decorateRequest(ContainerRequestContext requestContext, Span span);

    /**
     * Decorate spans by outgoing object.
     *
     * @param responseContext
     * @param span
     */
    void decorateResponse(ContainerResponseContext responseContext, Span span);

    /**
     * Adds following attributes to the span:
     * {@link SemanticAttributes#HTTP_METHOD}, {@link SemanticAttributes#HTTP_URL} and
     * {@link SemanticAttributes#HTTP_STATUS_CODE}
     */
    io.smallrye.opentelemetry.jaxrs2.server.ServerSpanDecorator STANDARD_TAGS = new io.smallrye.opentelemetry.jaxrs2.server.ServerSpanDecorator() {
        @Override
        public void decorateRequest(ContainerRequestContext requestContext, Span span) {
            SemanticAttributes.HTTP_METHOD.set(span, requestContext.getMethod());

            String url = URIUtils.url(requestContext.getUriInfo().getRequestUri());
            if (url != null) {
                SemanticAttributes.HTTP_URL.set(span, url);
            }
        }

        @Override
        public void decorateResponse(ContainerResponseContext responseContext, Span span) {
            SemanticAttributes.HTTP_STATUS_CODE.set(span, responseContext.getStatus());
        }
    };
}
