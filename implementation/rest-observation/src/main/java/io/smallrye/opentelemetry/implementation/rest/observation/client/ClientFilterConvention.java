package io.smallrye.opentelemetry.implementation.rest.observation.client;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

public interface ClientFilterConvention extends ObservationConvention<ObservationClientContext> {
    @Override
    default boolean supportsContext(Observation.Context context) {
        return context instanceof ObservationClientContext;
    }
}
