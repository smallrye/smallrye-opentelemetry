package io.smallrye.opentelemetry.observation.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class TestConfigSource implements ConfigSource {
    static final Map<String, String> configuration = new HashMap<>();

    @Override
    public Set<String> getPropertyNames() {
        return configuration.keySet();
    }

    @Override
    public String getValue(final String propertyName) {
        return configuration.get(propertyName);
    }

    @Override
    public String getName() {
        return TestConfigSource.class.getName();
    }
}
