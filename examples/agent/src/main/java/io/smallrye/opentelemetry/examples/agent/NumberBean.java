package io.smallrye.opentelemetry.examples.agent;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
