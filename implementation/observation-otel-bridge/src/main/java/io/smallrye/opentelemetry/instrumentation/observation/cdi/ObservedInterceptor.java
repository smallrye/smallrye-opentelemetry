package io.smallrye.opentelemetry.instrumentation.observation.cdi;

import java.lang.reflect.Method;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import io.smallrye.opentelemetry.instrumentation.observation.cdi.convention.CdiInterceptorContext;
import io.smallrye.opentelemetry.instrumentation.observation.cdi.convention.DefaultObservedInterceptorConvention;
import io.smallrye.opentelemetry.instrumentation.observation.cdi.convention.ObservedInterceptorConvention;
import io.smallrye.opentelemetry.instrumentation.observation.cdi.convention.ObservedInterceptorDocumentation;

public class ObservedInterceptor {
    private final ObservationRegistry registry;
    private final ObservedInterceptorConvention customUserConvention;

    public ObservedInterceptor(final ObservationRegistry registry,
            final ObservedInterceptorConvention convention) {
        this.registry = registry;
        this.customUserConvention = convention;
    }

    @AroundInvoke
    public Object span(final InvocationContext invocationContext) throws Exception {
        return ObservedInterceptorDocumentation.DEFAULT
                .observation(customUserConvention,
                        new DefaultObservedInterceptorConvention(getName(invocationContext.getMethod())),
                        () -> new CdiInterceptorContext(invocationContext),
                        registry)
                .observeChecked(() -> invocationContext.proceed());
    }

    private String getName(Method method) {
        Observed annotation = method.getDeclaredAnnotation(Observed.class);
        return annotation.name().isEmpty() ? ("method.observed") : annotation.name();
    }
}
