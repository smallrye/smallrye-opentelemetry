package io.smallrye.opentelemetry.jaxrs2.server.internal;

import java.util.concurrent.atomic.AtomicBoolean;

import io.opentelemetry.context.Scope;
import io.opentelemetry.trace.Span;

/**
 * Wrapper class used for exchanging span between filters.
 *
 * @author Pavol Loffay
 */
public class SpanWrapper {

    public static final String PROPERTY_NAME = SpanWrapper.class.getName() + ".activeSpanWrapper";

    private Scope scope;
    private Span span;
    private AtomicBoolean finished = new AtomicBoolean();

    public SpanWrapper(Span span, Scope scope) {
        this.span = span;
        this.scope = scope;

    }

    public Span get() {
        return span;
    }

    public Scope getScope() {
        return scope;
    }

    public synchronized void finish() {
        if (!finished.get()) {
            finished.set(true);
            span.end();
        }
    }

    public boolean isFinished() {
        return finished.get();
    }
}
