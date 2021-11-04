package io.smallrye.opentelemetry.implementation.cdi;

import static java.util.Collections.emptyList;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;

@ApplicationScoped
public class OpenTelemetryConfigProperties implements ConfigProperties {
    @Inject
    Config config;

    @Override
    public String getString(final String name) {
        return config.getOptionalValue(name, String.class).orElse(null);
    }

    @Override
    public Boolean getBoolean(final String name) {
        return config.getOptionalValue(name, Boolean.class).orElse(null);
    }

    @Override
    public Integer getInt(final String name) {
        return config.getOptionalValue(name, Integer.class).orElse(null);
    }

    @Override
    public Long getLong(final String name) {
        return config.getOptionalValue(name, Long.class).orElse(null);
    }

    @Override
    public Double getDouble(final String name) {
        return config.getOptionalValue(name, Double.class).orElse(null);
    }

    @Override
    public Duration getDuration(final String name) {
        // TODO - May require a Duration Converter. Need to check how it is supposed to be set in OpenTel
        return config.getOptionalValue(name, Duration.class).orElse(null);
    }

    @Override
    public List<String> getList(final String name) {
        return config.getOptionalValues(name, String.class).orElse(emptyList());
    }

    @Override
    public Map<String, String> getMap(final String name) {
        // TODO - This is how OTel Config sets maps, but maybe we can use SR Config way
        Map<String, String> values = new HashMap<>();
        List<String> keyValues = getList(name);
        for (String keyValue : keyValues) {
            String[] split = keyValue.split("=");
            if (split.length != 2) {
                throw new ConfigurationException("Invalid map property: " + name + "=" + getString(name));
            }

            String key = split[0].trim();
            String value = split[1].trim();

            if (!key.isEmpty() && !value.isEmpty()) {
                values.put(split[0], split[1]);
            }
        }
        return values;
    }
}
