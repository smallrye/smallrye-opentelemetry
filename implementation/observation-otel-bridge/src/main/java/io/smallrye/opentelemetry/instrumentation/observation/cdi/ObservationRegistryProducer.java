package io.smallrye.opentelemetry.instrumentation.observation.cdi;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.smallrye.opentelemetry.instrumentation.observation.handler.OpenTelemetryObservationHandler;
import io.smallrye.opentelemetry.instrumentation.observation.handler.PropagatingReceiverTracingObservationHandler;
import io.smallrye.opentelemetry.instrumentation.observation.handler.PropagatingSenderTracingObservationHandler;
import io.smallrye.opentelemetry.instrumentation.observation.handler.TracingAwareMeterObservationHandler;

@Singleton
public class ObservationRegistryProducer {
    @Inject
    Tracer tracer;

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    MeterRegistry meterRegistry;

    @Produces
    @Singleton
    public ObservationRegistry registry() {
        final ObservationRegistry observationRegistry = ObservationRegistry.create();

        observationRegistry.observationConfig()
                //        .observationFilter(new CloudObservationFilter())  // Where global filters go
                //        .observationConvention(new GlobalTaxObservationConvention())  Where global conventions go
                .observationHandler(new ObservationHandler.FirstMatchingCompositeObservationHandler(
                        new PropagatingSenderTracingObservationHandler(tracer,
                                openTelemetry.getPropagators().getTextMapPropagator()),
                        new PropagatingReceiverTracingObservationHandler(tracer,
                                openTelemetry.getPropagators().getTextMapPropagator()),
                        new OpenTelemetryObservationHandler(tracer)))
                // todo duplicate the tracer strategy for baggage, adding a condition to bypass when no baggage is present
                // todo just implement the receiver and sender handlers
                // todo. Alternatively we can split in 2 the tracing handlers, one to create spans (in the current .observationHandler(new ObservationHandler.FirstMatchingCompositeObservationHandler )
                // todo. A new .observationHandler bloc to process the baggage on the receiver side.
                // todo. Another to propagate the context in a new .observationHandler )
                // todo. We assume on the receiver side we open and close the baggage once because it should have just 1 scope app wide and the
                // todo. user will use the baggage api itself. We are just making sure we don't break the propagation to the user.
                .observationHandler(
                        new TracingAwareMeterObservationHandler(new DefaultMeterObservationHandler(meterRegistry), tracer));
        //      .observationHandler(new PrintOutHandler())  // Can be implemented for debugging. Other handlers for future frameworks can also be added.
        return observationRegistry;
    }
}
