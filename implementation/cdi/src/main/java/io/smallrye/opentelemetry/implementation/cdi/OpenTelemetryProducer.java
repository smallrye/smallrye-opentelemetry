package io.smallrye.opentelemetry.implementation.cdi;

import static io.smallrye.opentelemetry.api.OpenTelemetryConfig.INSTRUMENTATION_NAME;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkAutoConfiguration;

@Singleton
public class OpenTelemetryProducer {
    @Inject
    OpenTelemetryConfigProperties configProperties;

    @Produces
    @Singleton
    public OpenTelemetry getOpenTelemetry() {
        // TODO - Register exporters as CDI Beans?
        // TODO - We need some changes in the auto configuration code, so we can customize it a bit better
        // TODO - Careful that auto configuration adds a shutdown hook here: io/opentelemetry/sdk/autoconfigure/TracerProviderConfiguration.java:58
        return OpenTelemetrySdkAutoConfiguration.initialize(true, configProperties);
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
