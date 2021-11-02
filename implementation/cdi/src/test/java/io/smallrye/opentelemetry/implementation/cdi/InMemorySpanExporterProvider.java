package io.smallrye.opentelemetry.implementation.cdi;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class InMemorySpanExporterProvider implements ConfigurableSpanExporterProvider {
    @Override
    public SpanExporter createExporter(final ConfigProperties config) {
        return InMemorySpanExporter.HOLDER.get();
    }

    @Override
    public String getName() {
        return "in-memory";
    }
}
