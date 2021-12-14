package io.smallrye.opentelemetry.implementation.cdi;

import static java.util.Collections.emptyList;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.Converter;

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
        return config.getOptionalValue(name, String.class).map(DURATION_CONVERTER::convert).orElse(null);
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

    private static final Converter<Duration> DURATION_CONVERTER = new DurationConverter();

    /**
     * Mostly a copy from OTel Duration conversion. Check io.opentelemetry.sdk.autoconfigure.DefaultConfigProperties.
     */
    private static class DurationConverter implements Converter<Duration> {
        @Override
        public Duration convert(final String value) throws IllegalArgumentException, NullPointerException {
            if (value == null || value.isEmpty()) {
                return null;
            }

            String unitString = getUnitString(value);
            String numberString = value.substring(0, value.length() - unitString.length());
            try {
                long rawNumber = Long.parseLong(numberString.trim());
                TimeUnit unit = getDurationUnit(unitString.trim());
                return Duration.ofMillis(TimeUnit.MILLISECONDS.convert(rawNumber, unit));
            } catch (NumberFormatException | ConfigurationException e) {
                throw new IllegalArgumentException(e);
            }
        }

        private static String getUnitString(String rawValue) {
            int lastDigitIndex = rawValue.length() - 1;
            while (lastDigitIndex >= 0) {
                char c = rawValue.charAt(lastDigitIndex);
                if (Character.isDigit(c)) {
                    break;
                }
                lastDigitIndex -= 1;
            }
            // Pull everything after the last digit.
            return rawValue.substring(lastDigitIndex + 1);
        }

        private static TimeUnit getDurationUnit(String unitString) {
            switch (unitString) {
                case "": // Fallthrough expected
                case "ms":
                    return TimeUnit.MILLISECONDS;
                case "s":
                    return TimeUnit.SECONDS;
                case "m":
                    return TimeUnit.MINUTES;
                case "h":
                    return TimeUnit.HOURS;
                case "d":
                    return TimeUnit.DAYS;
                default:
                    throw new ConfigurationException("Invalid duration string, found: " + unitString);
            }
        }
    }
}
