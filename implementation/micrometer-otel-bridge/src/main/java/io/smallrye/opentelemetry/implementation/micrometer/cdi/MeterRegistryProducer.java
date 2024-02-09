package io.smallrye.opentelemetry.implementation.micrometer.cdi;

import java.util.concurrent.TimeUnit;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry;

@Singleton
public class MeterRegistryProducer {

    @Inject
    OpenTelemetry openTelemetry;

    @Produces
    @Singleton
    public MeterRegistry createRegistry(final OpenTelemetry openTelemetry) {
        final MeterRegistry meterRegistry = OpenTelemetryMeterRegistry.builder(openTelemetry)
                .setPrometheusMode(false)
                .setMicrometerHistogramGaugesEnabled(true)
                .setBaseTimeUnit(TimeUnit.MILLISECONDS)
                .setClock(Clock.SYSTEM)
                .build();
        Metrics.addRegistry(meterRegistry);// FIXME how will this play with the global registry?
        return meterRegistry;
    }
}
