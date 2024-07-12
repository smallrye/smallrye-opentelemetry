package io.smallrye.opentelemetry.test;

import jakarta.enterprise.inject.spi.CDI;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class InMemorySpanExporterProvider implements ConfigurableSpanExporterProvider {
    @Override
    public SpanExporter createExporter(final ConfigProperties config) {
        return CDI.current().select(InMemorySpanExporter.class).get();
    }

    @Override
    public String getName() {
        return "in-memory";
    }
}
