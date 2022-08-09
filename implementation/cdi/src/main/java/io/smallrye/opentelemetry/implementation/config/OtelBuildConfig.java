package io.smallrye.opentelemetry.implementation.config;

import java.util.List;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.opentelemetry.implementation.config.traces.TracesBuildConfig;

@ConfigMapping(prefix = "otel")
//@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface OtelBuildConfig {

    String INSTRUMENTATION_NAME = "io.quarkus.opentelemetry";

    /**
     * If false, disable the OpenTelemetry usage at build time. All other Otel properties will
     * be ignored at runtime.
     * <p>
     * Will pick up value from legacy property quarkus.opentelemetry.enabled
     * <p>
     * Defaults to true.
     */
    @WithDefault("${quarkus.opentelemetry.enabled:true}") // FIXME use fallback
    Boolean enabled(); // fixme consolidate with the one on the runtime?

    /**
     * Trace exporter configurations
     */
    TracesBuildConfig traces();

    /**
     * No Metrics exporter for now
     */
    @WithDefault("none")
    @WithName("metrics.exporter")
    List<String> metricsExporter();// TODO create MetricsBuildConfig

    /**
     * No Log exporter for now
     */
    @WithDefault("none")
    @WithName("logs.exporter")
    List<String> logsExporter();// TODO create LogBuildConfig

    //    /**
    //     * The OpenTelemetry Protocol (OTLP) span, metric, and log exporters
    //     */
    //    @WithDefault(ExporterType.Constants.OTLP_VALUE)
    //    ExporterType exporter(); // TODO if exporter=otlp then ExporterOtlpTraceConfig must be present...

    /**
     * The propagators to be used. Use a comma-separated list for multiple propagators.
     * <p>
     * Has values from {@link PropagatorType} or the full qualified name of a class implementing
     * {@link io.opentelemetry.context.propagation.TextMapPropagator}
     * <p>
     * Default is {@value PropagatorType.Constants#TRACE_CONTEXT},{@value PropagatorType.Constants#BAGGAGE} (W3C).
     */
    @WithDefault(PropagatorType.Constants.TRACE_CONTEXT + "," + PropagatorType.Constants.BAGGAGE)
    List<String> propagators();
}
