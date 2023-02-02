package io.smallrye.opentelemetry.examples.agent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class NumberBean {
    @Inject
    @ConfigProperty(name = "generation.prefix", defaultValue = "UN")
    String prefix;

    public String getPrefix() {
        return prefix;
    }
}
