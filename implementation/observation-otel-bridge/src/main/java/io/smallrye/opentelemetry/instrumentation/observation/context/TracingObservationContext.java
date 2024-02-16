package io.smallrye.opentelemetry.instrumentation.observation.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.opentelemetry.api.trace.Span;

public class TracingObservationContext {

    private Span span;

    private Map<Thread, AutoCloseable> scopes = new ConcurrentHashMap<>();

    //    private Map<Thread, Map<String, String>> baggage = new ConcurrentHashMap<>();

    /**
     * Returns the span.
     *
     * @return span
     */
    public Span getSpan() {
        return this.span;
    }

    /**
     * Sets the span.
     *
     * @param span span to set
     */
    public void setSpan(Span span) {
        this.span = span;
    }

    /**
     * Returns the scope of the span.
     *
     * @return scope of the span
     */
    public AutoCloseable getScope() {
        return this.scopes.get(Thread.currentThread());
    }

    /**
     * Sets the current trace context scope.
     *
     * @param scope scope to set
     */
    public void setScope(AutoCloseable scope) {
        if (scope == null) {
            this.scopes.remove(Thread.currentThread());
        } else {
            this.scopes.put(Thread.currentThread(), scope);
        }
    }

    /**
     * Convenience method to set both span and scope.
     *
     * @param span span to set
     * @param scope scope to set
     */
    public void setSpanAndScope(Span span, ObservationScope scope) {
        setSpan(span);
        setScope(scope);
    }

    @Override
    public String toString() {
        return "TracingContext{" + "span=" + traceContextFromSpan() + '}';
    }

    private String traceContextFromSpan() {
        if (span != null) {
            return span.getSpanContext().toString();
        }
        return "null";
    }

}
