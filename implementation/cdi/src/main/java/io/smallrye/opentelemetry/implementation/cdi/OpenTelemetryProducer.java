package io.smallrye.opentelemetry.implementation.cdi;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

@Singleton
public class OpenTelemetryProducer {
    @Inject
    Instance<SpanExporter> exporters;

    @Produces
    @Singleton
    public OpenTelemetry getOpenTelemetry() {
        SdkTracerProviderBuilder tracerProviderBuilder = SdkTracerProvider.builder();
        for (SpanExporter exporter : exporters) {
            tracerProviderBuilder.addSpanProcessor(SimpleSpanProcessor.create(exporter));
        }
        return OpenTelemetrySdk.builder().setTracerProvider(tracerProviderBuilder.build()).buildAndRegisterGlobal();
    }
}
