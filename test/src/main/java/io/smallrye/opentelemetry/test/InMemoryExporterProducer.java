package io.smallrye.opentelemetry.test;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;

@Singleton
public class InMemoryExporterProducer {
    @Produces
    @Singleton
    public InMemorySpanExporter produceSpanExporter() {
        return InMemorySpanExporter.create();
    }

    @Produces
    @Singleton
    public InMemoryMetricExporter produceMetricsExporter() {
        return InMemoryMetricExporter.create();
    }

    @Produces
    @Singleton
    public InMemoryLogRecordExporter produceLogRecordExporter() {
        return InMemoryLogRecordExporter.create();
    }
}
