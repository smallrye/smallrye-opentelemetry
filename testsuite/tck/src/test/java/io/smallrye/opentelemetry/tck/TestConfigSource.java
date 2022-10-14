package io.smallrye.opentelemetry.tck;

import java.util.Map;

import io.smallrye.config.common.MapBackedConfigSource;

public class TestConfigSource extends MapBackedConfigSource {
    public TestConfigSource() {
        super("TestConfigSource",
                Map.of("otel.experimental.sdk.enabled", "false",
                        "otel.traces.exporter", "none",
                        "otel.metrics.exporter", "none"),
                Integer.MIN_VALUE);
    }
}
