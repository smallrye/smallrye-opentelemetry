package io.smallrye.opentelemetry.implementation.cdi;

import static io.smallrye.opentelemetry.api.OpenTelemetryConfig.INSTRUMENTATION_NAME;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;

@Singleton
public class OpenTelemetryProducer {
    @Inject
    Config config;

    @Produces
    @Singleton
    public OpenTelemetry getOpenTelemetry() {
        Map<String, String> oTelConfigs = new HashMap<>();
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith("otel.") || propertyName.startsWith("OTEL_")) {
                config.getOptionalValue(propertyName, String.class).ifPresent(
                        value -> oTelConfigs.put(propertyName, value));
            }
        }

        AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder();
        return builder.setResultAsGlobal(true)
                //.registerShutdownHook(false) -> Not released yet
                .addPropertiesSupplier(() -> oTelConfigs)
                .build()
                .getOpenTelemetrySdk();
    }

    @Produces
    @Singleton
    public Tracer getTracer() {
        return CDI.current().select(OpenTelemetry.class).get().getTracer(INSTRUMENTATION_NAME);
    }

    @Produces
    @RequestScoped
    public Span getSpan() {
        return Span.current();
    }

    @Produces
    @RequestScoped
    public Baggage getBaggage() {
        return Baggage.current();
    }
}
