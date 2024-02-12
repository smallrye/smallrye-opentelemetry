package io.smallrye.opentelemetry.instrumentation.observation.cdi.convention;

import jakarta.interceptor.InvocationContext;

import io.micrometer.observation.Observation;

/**
 * The context from the Observed interceptor.
 * This context will be used as the source for attributes values for the observation,
 * inside the Convention classes implementing ObservedInterceptorObservationConvention.
 */
public class ObservedInterceptorContext extends Observation.Context {
    private final InvocationContext invocationContext;

    public ObservedInterceptorContext(final InvocationContext invocationContext) {
        this.invocationContext = invocationContext;
    }

    public InvocationContext getInvocationContext() {
        return invocationContext;
    }

    //    // Don't put it here because this should be more generic.
    //    public String getMethodName() {
    //        return invocationContext.getMethod().getName();
    //    }
}
