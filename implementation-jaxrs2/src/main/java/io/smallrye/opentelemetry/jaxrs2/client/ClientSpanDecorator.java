package io.smallrye.opentelemetry.jaxrs2.client;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;

import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.attributes.SemanticAttributes;
import io.smallrye.opentelemetry.jaxrs2.server.internal.URIUtils;

/**
 * @author Pavol Loffay, Felix Wong
 */
public interface ClientSpanDecorator {

    /**
     * Decorate get by incoming object.
     *
     * @param requestContext
     * @param span
     */
    void decorateRequest(ClientRequestContext requestContext, Span span);

    /**
     * Decorate spans by outgoing object.
     *
     * @param responseContext
     * @param span
     */
    void decorateResponse(ClientResponseContext responseContext, Span span);

    /**
     * Adds standard tags: {@link io.opentracing.tag.Tags#SPAN_KIND},
     * {@link io.opentracing.tag.Tags#PEER_HOSTNAME}, {@link io.opentracing.tag.Tags#PEER_PORT},
     * {@link io.opentracing.tag.Tags#HTTP_METHOD}, {@link io.opentracing.tag.Tags#HTTP_URL} and
     * {@link io.opentracing.tag.Tags#HTTP_STATUS}
     */
    ClientSpanDecorator STANDARD_TAGS = new ClientSpanDecorator() {
        @Override
        public void decorateRequest(ClientRequestContext requestContext, Span span) {
            SemanticAttributes.HTTP_METHOD.set(span, requestContext.getMethod());
            String url = URIUtils.url(requestContext.getUri());
            if (url != null) {
                SemanticAttributes.HTTP_URL.set(span, url);
            }
        }

        @Override
        public void decorateResponse(ClientResponseContext responseContext, Span span) {
            SemanticAttributes.HTTP_STATUS_CODE.set(span, responseContext.getStatus());
        }
    };
}
