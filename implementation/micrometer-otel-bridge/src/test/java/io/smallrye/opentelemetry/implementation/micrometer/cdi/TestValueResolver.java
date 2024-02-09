package io.smallrye.opentelemetry.implementation.micrometer.cdi;

import jakarta.inject.Singleton;

import io.micrometer.common.annotation.ValueResolver;

@Singleton
public class TestValueResolver implements ValueResolver {
    @Override
    public String resolve(Object parameter) {
        return "prefix_" + parameter;
    }
}
