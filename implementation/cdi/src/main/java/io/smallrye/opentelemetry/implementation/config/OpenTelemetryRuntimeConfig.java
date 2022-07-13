package io.quarkus.opentelemetry.runtime;

import static io.quarkus.opentelemetry.runtime.OtelExporterUtil.*;

import java.util.Optional;

import io.quarkus.opentelemetry.runtime.tracing.TracesConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "otel", phase = ConfigPhase.RUN_TIME)
public class OpenTelemetryRuntimeConfig {

    /**
     * If false, disable the OpenTelemetry SDK.
     * <p>
     * Defaults to true.
     */
    @ConfigItem(defaultValue = "true")
    public boolean experimentalSdkEnabled;

    public TracesConfig traces;

    public AttributeConfig attributes;

    public OtelExporter exporter;

    public static class AttributeConfig {

        /**
         * The maximum length of attribute values. Applies to spans and logs.
         * <p>
         * By default there is no limit.
         */
        public Optional<String> valueLengthLimit;

        /**
         * The maximum number of attributes. Applies to spans, span events, span links, and logs.
         * <p>
         * Default is 128.
         */
        @ConfigItem(defaultValue = "128")
        public Integer countLimit;

    }

}
