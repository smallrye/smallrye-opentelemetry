package io.smallrye.opentelemetry.test;

import jakarta.enterprise.inject.spi.CDI;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;

public class InMemoryMetricExporterProvider implements ConfigurableMetricExporterProvider {
    @Override
    public MetricExporter createExporter(ConfigProperties configProperties) {
        return CDI.current().select(InMemoryMetricExporter.class).get();
    }

    @Override
    public String getName() {
        return "in-memory";
    }
}
