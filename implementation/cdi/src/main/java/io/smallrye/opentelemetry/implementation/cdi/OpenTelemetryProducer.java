package io.smallrye.opentelemetry.implementation.cdi;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.opentelemetry.api.OpenTelemetry;
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
}
