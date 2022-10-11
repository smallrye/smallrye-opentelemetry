package io.smallrye.opentelemetry.examples.jaeger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
