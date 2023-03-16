package io.smallrye.opentelemetry.tck;

import java.util.Map;

import io.smallrye.config.common.MapBackedConfigSource;

public class TestConfigSource extends MapBackedConfigSource {
    public TestConfigSource() {
        super("TestConfigSource",
                Map.of("otel.sdk.disabled", "true",
                        "otel.traces.exporter", "none",
                        "otel.metrics.exporter", "none"),
                Integer.MIN_VALUE);
    }
}
