package io.smallrye.opentelemetry.implementation.rest.observation.server;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

public interface ServerFilterConvention extends ObservationConvention<ObservationServerContext> {
    @Override
    default boolean supportsContext(Observation.Context context) {
        return context instanceof ObservationServerContext;
    }
}
