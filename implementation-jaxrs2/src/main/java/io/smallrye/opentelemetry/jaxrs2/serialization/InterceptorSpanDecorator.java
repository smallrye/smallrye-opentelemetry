package io.smallrye.opentelemetry.jaxrs2.serialization;

import javax.ws.rs.ext.InterceptorContext;

import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.attributes.StringAttributeSetter;

public interface InterceptorSpanDecorator {
    /**
     * Decorate spans by outgoing object.
     *
     * @param context
     * @param span
     */
    void decorateRead(InterceptorContext context, Span span);

    /**
     * Decorate spans by outgoing object.
     *
     * @param context
     * @param span
     */
    void decorateWrite(InterceptorContext context, Span span);

    /**
     * Adds tags: \"media.type\", \"entity.type\"
     */
    class StandardTags implements InterceptorSpanDecorator {
        StringAttributeSetter MEDIA_TYPE_ATTRIBUTE = StringAttributeSetter.create("media.type");
        StringAttributeSetter ENTITY_TYPE_ATTRIBUTE = StringAttributeSetter.create("entity.type");

        @Override
        public void decorateRead(InterceptorContext context, Span span) {
            MEDIA_TYPE_ATTRIBUTE.set(span, context.getMediaType().toString());
            ENTITY_TYPE_ATTRIBUTE.set(span, context.getType().getName());
        }

        @Override
        public void decorateWrite(InterceptorContext context, Span span) {
            decorateRead(context, span);
        }
    }
}
