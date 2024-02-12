package io.smallrye.opentelemetry.instrumentation.observation.cdi.convention;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * The convention for the Observed interceptor.
 * The user can override this class to provide a custom convention, if not,
 * the DefaultObservedInterceptorObservationConvention will be used.
 */
public interface ObservedInterceptorObservationConvention extends ObservationConvention<ObservedInterceptorContext> {
    @Override
    default boolean supportsContext(Observation.Context context) {
        return context instanceof ObservedInterceptorContext;
    }
}
