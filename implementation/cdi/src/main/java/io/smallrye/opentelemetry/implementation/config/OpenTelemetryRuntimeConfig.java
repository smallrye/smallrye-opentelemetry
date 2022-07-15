package io.smallrye.opentelemetry.implementation.config;

import static io.smallrye.opentelemetry.implementation.config.OpenTelemetryRuntimeConfig.PropagatorType.Constants.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.opentelemetry.implementation.config.traces.TracesConfig;

@ConfigMapping(prefix = "otel")
public interface OpenTelemetryRuntimeConfig {

    /**
     * If false, disable the OpenTelemetry SDK.
     * <p>
     * Defaults to true.
     */
    @WithDefault("true")
    @WithName("experimental.sdk.enabled")
    Boolean experimentalSdkEnabled(); // FIXME deploy config

    TracesConfig traces();

    /**
     * The OpenTelemetry Protocol (OTLP) span, metric, and log exporters
     */
    ExporterType exporter(); // TODO if exporter=otlp then ExporterOtlpTraceConfig must be present...
    // FIXME deploy config

    AttributeConfig attributes();

    SpanConfig span();

    BatchSpanProcessorConfig bsp();

    /**
     * Specify resource attributes in the following format: key1=val1,key2=val2,key3=val3
     */
    @WithName("resource.attributes")
    Map<String, String> resourceAttributes();

    /**
     * Specify logical service name. Takes precedence over service.name defined with otel.resource.attributes.
     */
    @WithName("service.name")
    Optional<String> serviceName(); //FIXME should come from quarkus service name. Implement on processor.

    /**
     * Specify resource attribute keys that are filtered.
     */
    @WithName("experimental.resource.disabled-keys")
    Optional<List<String>> experimentalResourceDisabledKeys();

    /**
     * The propagators to be used. Use a comma-separated list for multiple propagators.
     * <p>
     * Has values from {@link PropagatorType} or the full qualified name of a class implementing
     * {@link io.opentelemetry.context.propagation.TextMapPropagator}
     * <p>
     * Default is {@value PropagatorType.Constants#TRACE_CONTEXT},{@value PropagatorType.Constants#BAGGAGE} (W3C).
     */
    @WithDefault(TRACE_CONTEXT + "," + BAGGAGE)
    List<String> propagators(); // FIXME deploy config

    //********************************** inner structures ***************************

    interface AttributeConfig {

        /**
         * The maximum length of attribute values. Applies to spans and logs.
         * <p>
         * By default there is no limit.
         */
        @WithName("value.length.limit")
        Optional<String> valueLengthLimit();

        /**
         * The maximum number of attributes. Applies to spans, span events, span links, and logs.
         * <p>
         * Default is 128.
         */
        @WithName("count.limit")
        @WithDefault("128")
        Integer countLimit();

    }

    interface SpanConfig {
        /**
         * The maximum length of span attribute values. Takes precedence over otel.attribute.value.length.limit.
         * <p>
         * By default there is no limit.
         */
        @WithName("attribute.value.length.limit")
        Optional<Integer> attributeValueLengthLimit();

        /**
         * The maximum number of attributes per span. Takes precedence over otel.attribute.count.limit.
         * <p>
         * Default is 128.
         */
        @WithName("attribute.count.limit")
        @WithDefault("128")
        Integer attributeCountLimit();

        /**
         * The maximum number of events per span.
         * <p>
         * Default is 128.
         */
        @WithName("event.count.limit")
        @WithDefault("128")
        Integer eventCountLimit();

        /**
         * The maximum number of links per span.
         * <p>
         * Default is 128.
         */
        @WithName("link.count.limit")
        @WithDefault("128")
        Integer linkCountLimit();
    }

    enum PropagatorType {
        TRACE_CONTEXT(Constants.TRACE_CONTEXT),
        BAGGAGE(Constants.BAGGAGE),
        B3(Constants.B3),
        B3MULTI(Constants.B3MULTI),
        JAEGER(Constants.JAEGER),
        XRAY(Constants.XRAY),
        OT_TRACE(Constants.OT_TRACE);

        private String value;

        PropagatorType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        class Constants {
            public static final String TRACE_CONTEXT = "tracecontext";
            public static final String BAGGAGE = "baggage";
            public static final String B3 = "b3";
            public static final String B3MULTI = "b3multi";
            public static final String JAEGER = "jaeger";
            public static final String XRAY = "xray";
            public static final String OT_TRACE = "ottrace";
        }
    }

    interface BatchSpanProcessorConfig {
        /**
         * The interval, in milliseconds, between two consecutive exports.
         * <p>
         * Default is 5000.
         */
        @WithName("schedule.delay")
        @WithDefault("5000")
        Integer scheduleDelay();

        /**
         * The maximum queue size.
         * <p>
         * Default is 2048.
         */
        @WithName("max.queue.size")
        @WithDefault("2048")
        Integer maxQueueSize();

        /**
         * The maximum batch size.
         * <p>
         * Default is 512.
         */
        @WithName("max.export.batch.size")
        @WithDefault("512")
        Integer maxExportBatchSize();

        /**
         * The maximum allowed time, in milliseconds, to export data.
         * <p>
         * Default is 30_000.
         */
        @WithName("export.timeout")
        @WithDefault("30000")
        Integer exportTimeout();
    }

    public enum ExporterType {
        OTLP(Constants.OTLP_VALUE),
        //        HTTP(Constants.HTTP_VALUE), // TODO not supported yet
        JAEGER(Constants.JAEGER),
        NONE(Constants.NONE_VALUE);

        private String value;

        ExporterType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static class Constants {
            public static final String OTLP_VALUE = "otlp";
            //            public static final String HTTP_VALUE = "http";
            public static final String NONE_VALUE = "none";
            public static final String JAEGER = "jaeger";
        }
    }
}
