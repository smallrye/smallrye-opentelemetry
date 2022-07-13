package io.smallrye.opentelemetry.implementation.config.traces;

import io.smallrye.config.WithDefault;
import io.smallrye.opentelemetry.implementation.config.OpenTelemetryRuntimeConfig.ExporterType;

import java.util.List;
import java.util.Optional;

import static io.smallrye.opentelemetry.implementation.config.OpenTelemetryRuntimeConfig.ExporterType.Constants.OTLP_VALUE;

public interface TracesConfig {

    /**
     * List of exporters supported by Quarkus.
     * <p>
     * List of exporters to be used for tracing, separated by commas. none means no autoconfigured exporter.
     * <p>
     * Default is OTLP.
     *
     * @return
     */
    @WithDefault(OTLP_VALUE)
    ExporterType exporter();

    SamplerConfig sampler();

    interface SamplerConfig {
        /**
         * The sampler to use for tracing.
         * <p>
         * Defaults to {@value SamplerType.Constants#PARENT_BASED_ALWAYS_ON}
         */
        @WithDefault(SamplerType.Constants.PARENT_BASED_ALWAYS_ON)
        SamplerType sampler(); // FIXME same as top level. Must be otel.traces.sampler

        /**
         * An argument to the configured tracer if supported, for example a ratio.
         */
        Optional<List<String>> arg();
    }

    enum SamplerType {
        ALWAYS_ON(Constants.ALWAYS_ON),
        ALWAYS_OFF(Constants.ALWAYS_OFF),
        TRACE_ID_RATIO(Constants.TRACE_ID_RATIO),
        PARENT_BASED_ALWAYS_ON(Constants.PARENT_BASED_ALWAYS_ON),
        PARENT_BASED_ALWAYS_OFF(Constants.PARENT_BASED_ALWAYS_OFF),
        PARENT_BASED_TRACE_ID_RATIO(Constants.PARENT_BASED_TRACE_ID_RATIO);

        private String value;

        SamplerType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        static class Constants {
            public static final String ALWAYS_ON = "always_on";
            public static final String ALWAYS_OFF = "always_off";
            public static final String TRACE_ID_RATIO = "traceidratio";
            public static final String PARENT_BASED_ALWAYS_ON = "parentbased_always_on";
            public static final String PARENT_BASED_ALWAYS_OFF = "parentbased_always_off";
            public static final String PARENT_BASED_TRACE_ID_RATIO = "parentbased_traceidratio";
        }
    }
}
