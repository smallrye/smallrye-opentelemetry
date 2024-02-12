package io.smallrye.opentelemetry.instrumentation.observation.cdi.convention;

import java.lang.reflect.Method;

import io.micrometer.common.KeyValues;
import io.micrometer.observation.annotation.Observed;

/**
 * Default implementation of the {@link ObservedInterceptorObservationConvention}, using the default names according
 * to the ObservedInterceptorObservationDocumentation.
 */
public class DefaultObservedInterceptorObservationConvention implements ObservedInterceptorObservationConvention {
    private final String name;

    public DefaultObservedInterceptorObservationConvention(final String name) {
        this.name = name;
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ObservedInterceptorContext context) {

        KeyValues annotationKeyValues = getAnnotationKeyValues(context.getInvocationContext().getMethod());
        return annotationKeyValues
                .and(ObservedInterceptorObservationDocumentation.ObservedKeyValues.CODE_NAMESPACE.withValue(
                        context.getInvocationContext().getMethod().getDeclaringClass().getName()))
                .and(ObservedInterceptorObservationDocumentation.ObservedKeyValues.CODE_FUNCTION.withValue(
                        context.getInvocationContext().getMethod().getName()));
    }

    //    Not needed for Observed
    //    @Override
    //    public KeyValues getHighCardinalityKeyValues(ObservedInterceptorContext context) {
    //        return ObservedInterceptorObservationConvention.super.getHighCardinalityKeyValues(context);
    //    }

    @Override
    public String getName() {
        // this is the metric name
        return name;
    }

    @Override
    public String getContextualName(ObservedInterceptorContext context) {
        // this is for spans
        return contextualName(context.getInvocationContext().getMethod());
    }

    private KeyValues getAnnotationKeyValues(Method method) {
        String[] keyValues = method.getAnnotation(Observed.class).lowCardinalityKeyValues();
        if (keyValues.length == 0) {
            return KeyValues.empty();
        }
        return KeyValues.of(keyValues);
    }

    private String contextualName(Method method) {
        Observed annotation = method.getDeclaredAnnotation(Observed.class);
        return annotation.contextualName().isEmpty()
                ? (method.getDeclaringClass().getSimpleName() + "#" + method.getName())
                : annotation.contextualName();
    }
}
