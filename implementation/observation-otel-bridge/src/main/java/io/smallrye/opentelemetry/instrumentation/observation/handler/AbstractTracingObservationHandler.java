package io.smallrye.opentelemetry.instrumentation.observation.handler;

import static io.opentelemetry.semconv.SemanticAttributes.NET_SOCK_PEER_ADDR;
import static io.opentelemetry.semconv.SemanticAttributes.NET_SOCK_PEER_PORT;
import static io.opentelemetry.semconv.SemanticAttributes.PEER_SERVICE;
import static io.smallrye.opentelemetry.instrumentation.observation.handler.HandlerUtil.HIGH_CARD_ATTRIBUTES;
import static io.smallrye.opentelemetry.instrumentation.observation.handler.HandlerUtil.LOW_CARD_ATTRIBUTES;

import java.net.URI;
import java.util.logging.Logger;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationView;
import io.micrometer.observation.transport.Kind;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Scope;
import io.smallrye.opentelemetry.instrumentation.observation.context.TracingObservationContext;

abstract class AbstractTracingObservationHandler<T extends Observation.Context> implements ObservationHandler<T> {

    private static final Logger logger = Logger.getLogger(AbstractTracingObservationHandler.class.getName());

    @Override
    public void onError(T context) {
        if (context.getError() != null) {
            getRequiredSpan(context).recordException(context.getError());
        }
    }

    @Override
    public void onEvent(Observation.Event event, T context) {
        AttributesBuilder attributesBuilder = Attributes.builder();
        context.getAllKeyValues().forEach(keyValue -> {
            attributesBuilder.put(AttributeKey.stringKey(keyValue.getKey()), keyValue.getValue());
        });
        getRequiredSpan(context).addEvent(event.getName(), attributesBuilder.build());

    }

    @Override
    public void onScopeOpened(T context) {
        TracingObservationContext tracingContext = getTracingContext(context);
        Span span = tracingContext.getSpan();
        Scope scope = span.makeCurrent();
        tracingContext.setScope(scope);
    }

    @Override
    public void onScopeClosed(T context) {
        TracingObservationContext tracingContext = getTracingContext(context);
        try {
            AutoCloseable scope = tracingContext.getScope();
            if (scope != null) {
                scope.close();
            } else {
                logger.warning("Scope is null for observation context name:" + context.getName());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onScopeReset(T context) {
        logger.warning("ScopeReset Should not happen nor be needed. Observation context name:" + context.getName());
    }

    protected TracingObservationContext getTracingContext(T context) {
        return context.computeIfAbsent(TracingObservationContext.class, clazz -> new TracingObservationContext());
    }

    protected Span getRequiredSpan(T context) {
        Span span = getTracingContext(context).getSpan();
        if (span == null) {
            throw new IllegalStateException("Span wasn't started - an observation must be started (not only created)");
        }
        return span;
    }

    protected Span getParentSpan(T context) {
        // This would mean that the user has manually created a tracing context
        TracingObservationContext tracingContext = context.get(TracingObservationContext.class);
        Span currentSpan = Span.current();
        if (tracingContext == null) {
            ObservationView observation = context.getParentObservation();
            if (observation != null) {
                tracingContext = observation.getContextView().get(TracingObservationContext.class);
                if (tracingContext != null) {
                    Span spanFromParentObservation = tracingContext.getSpan();
                    if (spanFromParentObservation == null && currentSpan != null) {
                        return currentSpan;
                    } else if (currentSpan != null && !currentSpan.equals(spanFromParentObservation)) {
                        // User manually created a span
                        return currentSpan;
                    }
                    // No manually created span
                    return spanFromParentObservation;
                }
            }
        } else {
            return tracingContext.getSpan();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected void tagSpan(T context, Span span) {
        final Attributes highCardAttributes = context.get(HIGH_CARD_ATTRIBUTES);
        setOtelAttributes(span, highCardAttributes);

        final Attributes lowCardAttributes = context.get(LOW_CARD_ATTRIBUTES);
        setOtelAttributes(span, lowCardAttributes);

        for (KeyValue keyValue : context.getAllKeyValues()) {
            if (!keyValue.getKey().equalsIgnoreCase("ERROR")) {
                span.setAttribute(keyValue.getKey(), keyValue.getValue());

            } else {
                span.recordException(new RuntimeException(keyValue.getValue()));
            }
        }
    }

    private void setOtelAttributes(Span span, Attributes contextAttributes) {
        if (contextAttributes != null) {
            contextAttributes.forEach((key, value) -> {
                // FIXME this is a bit of a hack because KeyValue only allows String values
                if (key.getKey().equalsIgnoreCase("ERROR")) {
                    span.recordException(new RuntimeException(value.toString()));
                } else {
                    span.setAttribute((AttributeKey<Object>) key, value);
                }
            });
        }
    }

    protected SpanBuilder remoteSpanBuilder(Kind kind,
            String remoteServiceName,
            String remoteServiceAddress,
            SpanBuilder builder) {
        builder.setSpanKind(kind(kind));

        if (remoteServiceName != null) {
            builder = builder.setAttribute(PEER_SERVICE, remoteServiceName);
        }
        if (remoteServiceAddress != null) {
            try {
                URI uri = URI.create(remoteServiceAddress);
                builder = builder.setAttribute(NET_SOCK_PEER_ADDR, uri.getHost());
                builder = builder.setAttribute(NET_SOCK_PEER_PORT, Long.valueOf(uri.getPort()));
            } catch (Exception ex) {
                logger.warning("Exception [{}], occurred while trying to parse" +
                        " the uri [{}] to host and port." + remoteServiceAddress +
                        ex);
            }
        }
        return builder;
    }

    protected SpanKind kind(Kind observationKind) {
        if (observationKind == null) {
            return SpanKind.INTERNAL;
        }
        switch (observationKind) {
            case CLIENT:
                return SpanKind.CLIENT;
            case SERVER:
                return SpanKind.SERVER;
            case PRODUCER:
                return SpanKind.PRODUCER;
            case CONSUMER:
                return SpanKind.CONSUMER;
            default:
                return SpanKind.INTERNAL;
        }
    }
}
