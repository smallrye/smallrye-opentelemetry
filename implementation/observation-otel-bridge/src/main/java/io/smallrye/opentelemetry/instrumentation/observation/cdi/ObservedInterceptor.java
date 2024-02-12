package io.smallrye.opentelemetry.instrumentation.observation.cdi;

import java.lang.reflect.Method;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import io.smallrye.opentelemetry.instrumentation.observation.cdi.convention.DefaultObservedInterceptorObservationConvention;
import io.smallrye.opentelemetry.instrumentation.observation.cdi.convention.ObservedInterceptorContext;
import io.smallrye.opentelemetry.instrumentation.observation.cdi.convention.ObservedInterceptorObservationConvention;
import io.smallrye.opentelemetry.instrumentation.observation.cdi.convention.ObservedInterceptorObservationDocumentation;

public class ObservedInterceptor {
    private final ObservationRegistry registry;
    private final ObservedInterceptorObservationConvention customUserConvention;

    public ObservedInterceptor(final ObservationRegistry registry,
            final ObservedInterceptorObservationConvention convention) {
        this.registry = registry;
        this.customUserConvention = convention;
    }

    @AroundInvoke
    public Object span(final InvocationContext invocationContext) throws Exception {
        return ObservedInterceptorObservationDocumentation.DEFAULT
                .observation(customUserConvention,
                        new DefaultObservedInterceptorObservationConvention(getName(invocationContext.getMethod())),
                        () -> new ObservedInterceptorContext(invocationContext),
                        registry)
                .observeChecked(() -> invocationContext.proceed());
    }

    private String getName(Method method) {
        Observed annotation = method.getDeclaredAnnotation(Observed.class);
        return annotation.name().isEmpty() ? ("method.observed") : annotation.name();
    }
}
