package io.smallrye.opentelemetry.instrumentation.observation.context;

interface ObservationScope extends AutoCloseable {
    /**
     * A noop implementation.
     */
    ObservationScope NOOP = () -> {

    };

    @Override
    void close();
}
