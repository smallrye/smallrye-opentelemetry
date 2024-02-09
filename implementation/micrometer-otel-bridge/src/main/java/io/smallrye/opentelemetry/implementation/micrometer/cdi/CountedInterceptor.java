package io.smallrye.opentelemetry.implementation.micrometer.cdi;

import java.lang.reflect.Method;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

public class CountedInterceptor {

    public static final String DEFAULT_EXCEPTION_TAG_VALUE = "none";
    public static final String RESULT_TAG_FAILURE_VALUE = "failure";
    public static final String RESULT_TAG_SUCCESS_VALUE = "success";

    private final MeterRegistry meterRegistry;

    public CountedInterceptor(final MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @AroundInvoke
    public Object intercept(final InvocationContext context) throws Exception {

        Counted counted = context.getMethod().getAnnotation(Counted.class);
        Tags tags = getCommonTags(context);

        try {
            Object result = context.proceed();
            if (!counted.recordFailuresOnly()) {
                record(counted, tags, null);
            }
            return result;
        } catch (Throwable e) {
            record(counted, tags, e);
            throw e;
        }
    }

    private void record(Counted counted, Tags commonTags, Throwable throwable) {
        Counter.Builder builder = Counter.builder(counted.value())
                .baseUnit("invocations")
                .tags(commonTags)
                .tags(counted.extraTags())
                .tag("exception", getExceptionTag(throwable))
                .tag("result", throwable == null ? RESULT_TAG_SUCCESS_VALUE : RESULT_TAG_FAILURE_VALUE);
        String description = counted.description();
        if (!description.isEmpty()) {
            builder.description(description);
        } else {
            builder.description("Total number of invocations for method");
        }
        builder.register(meterRegistry).increment();
    }

    private String getExceptionTag(Throwable throwable) {
        if (throwable == null) {
            return DEFAULT_EXCEPTION_TAG_VALUE;
        }
        if (throwable.getCause() == null) {
            return throwable.getClass().getSimpleName();
        }
        return throwable.getCause().getClass().getSimpleName();
    }

    private static Tags getCommonTags(InvocationContext context) {
        Method method = context.getMethod();
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();
        return Tags.of("class", className, "method", methodName);
    }
}
