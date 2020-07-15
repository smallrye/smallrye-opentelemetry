package io.smallrye.opentelemetry.example.resteasy;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Application;

import io.opentelemetry.exporters.logging.LoggingSpanExporter;
import io.opentelemetry.exporters.otlp.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.smallrye.opentelemetry.jaxrs2.server.ServerTracingDynamicFeature;

/**
 * @author Pavol Loffay
 */
public class ExampleApplication extends Application {
    private final HashSet<Object> singletons = new HashSet<>();

    public ExampleApplication() {
        TracerSdkProvider tracerProvider = OpenTelemetrySdk.getTracerProvider();
        TracerSdkProvider.builder().build();

        // add logging exporter
        tracerProvider.addSpanProcessor(SimpleSpanProcessor.newBuilder(new LoggingSpanExporter()).build());
        // add OTLP exporter
        tracerProvider.addSpanProcessor(SimpleSpanProcessor.newBuilder(OtlpGrpcSpanExporter.getDefault()).build());
        DynamicFeature tracing = new ServerTracingDynamicFeature.Builder()
                .build();
        singletons.add(tracing);
        singletons.add(new ExampleResource());
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
