package io.smallrye.opentelemetry.examples.jaeger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.opentelemetry.instrumentation.annotations.WithSpan;

@ApplicationScoped
public class NumberBean {
    @Inject
    @ConfigProperty(name = "generation.prefix", defaultValue = "UN")
    String prefix;

    @WithSpan
    public String getPrefix() {
        return prefix;
    }
}
