package io.smallrye.opentelemetry.implementation.config;

import static io.smallrye.opentelemetry.implementation.common.KeyValuesConfigSource.config;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.inject.ConfigExtension;
import io.smallrye.opentelemetry.implementation.cdi.OpenTelemetryExtension;
import io.smallrye.opentelemetry.implementation.common.InMemorySpanExporter;

@EnableAutoWeld
@AddExtensions({ OpenTelemetryExtension.class, ConfigExtension.class })
public class AutoconfigureTest {
    public static SmallRyeConfig config;

    @BeforeAll
    static void beforeAll() {
        ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig());
        String[] properties = { "otel.experimental.sdk.enabled", "true",
                "otel.traces.enabled", "false",
                "otel.traces.exporter", "in-memory",
                "otel.exporter.otlp.endpoint", "endpoint",
                "otel.exporter.otlp.headers", "myheader=stuff,myheader2=stuff2",
                "otel.exporter.otlp.compression", "gzip",
                "otel.exporter.otlp.timeout", "1000",
                "otel.exporter.otlp.traces.endpoint", "traces.endpoint",
                "otel.exporter.otlp.traces.headers", "mytraceheader=tracestuff,mytraceheader2=tracestuff2",
                "otel.exporter.otlp.traces.compression", "none",
                "otel.exporter.otlp.traces.timeout", "2000",
                "otel.metrics.exporter", "none" };
        config = new SmallRyeConfigBuilder()
                .withSources(config(properties))
                .addDefaultInterceptors()
                .build();
        ConfigProviderResolver.instance().registerConfig(config, Thread.currentThread().getContextClassLoader());
    }

    @AfterAll
    static void afterAll() {
        ConfigProviderResolver.instance().releaseConfig(config);
    }

    private static Supplier<Map<String, String>> disableExportPropertySupplier() {
        Map<String, String> props = new HashMap<>();
        props.put("otel.metrics.exporter", "none");
        props.put("otel.traces.exporter", "none");
        props.put("otel.logs.exporter", "none");
        return () -> props;
    }

    @Inject
    private Config mpConfig;

    @Inject
    InMemorySpanExporter spanExporter; // needed in the classpath

    private AutoConfiguredOpenTelemetrySdkBuilder builder;

    @BeforeEach
    void resetGlobal() {
        GlobalOpenTelemetry.resetForTest();
        builder = AutoConfiguredOpenTelemetrySdk.builder()
                .setResultAsGlobal(false)
                .addPropertiesSupplier(disableExportPropertySupplier());
    }

    @Test
    public void basicAutoconfigureTest() {
        AutoConfiguredOpenTelemetrySdk autoConfigured = builder
                .addPropertiesSupplier(() -> Collections.singletonMap("key", "valueUnused"))
                .addPropertiesSupplier(() -> Collections.singletonMap("key", "value"))
                .addPropertiesSupplier(() -> Collections.singletonMap("otel-key", "otel-value"))
                .addPropertiesSupplier(
                        () -> Collections.singletonMap("otel.service.name", "test-service"))
                .setResultAsGlobal(false)
                .build();

        assertEquals("test-service",
                autoConfigured.getResource().getAttribute(ResourceAttributes.SERVICE_NAME));
        assertEquals("value",
                autoConfigured.getConfig().getString("key"));
        assertEquals("otel-value",
                autoConfigured.getConfig().getString("otel.key"));
        assertNotEquals("endpoint",
                autoConfigured.getConfig().getString("otel.exporter.otlp.endpoint"),
                "MP Config properties shouldn't be here");
    }

    @Test
    public void mpConfigToOtelConfigTest() {
        Map<String, String> mpConfigAttributes = new HashMap<>();
        mpConfig.getPropertyNames().forEach(name -> {
            ConfigValue value = mpConfig.getConfigValue(name);
            mpConfigAttributes.put(name, value.getValue());
        });

        AutoConfiguredOpenTelemetrySdk autoConfigured = builder
                .addPropertiesSupplier(() -> mpConfigAttributes)
                .setResultAsGlobal(false)
                .build();

        assertEquals(Boolean.TRUE,
                autoConfigured.getConfig().getBoolean("otel.experimental.sdk.enabled"));
        assertEquals(Boolean.FALSE,
                autoConfigured.getConfig().getBoolean(  "otel.traces.enabled"));
        assertEquals("in-memory",
                autoConfigured.getConfig().getString("otel.traces.exporter"));
        assertEquals("endpoint",
                autoConfigured.getConfig().getString("otel.exporter.otlp.endpoint"));
        assertThat(autoConfigured.getConfig().getMap("otel.exporter.otlp.headers"),
                both(hasEntry("myheader", "stuff"))
                        .and(hasEntry("myheader2", "stuff2")));
        assertEquals("gzip",
                autoConfigured.getConfig().getString("otel.exporter.otlp.compression"));
        assertEquals(1000,
                autoConfigured.getConfig().getInt("otel.exporter.otlp.timeout"));
        assertEquals("traces.endpoint",
                autoConfigured.getConfig().getString("otel.exporter.otlp.traces.endpoint"));
        assertThat(autoConfigured.getConfig().getMap("otel.exporter.otlp.traces.headers"),
                both(hasEntry("mytraceheader", "tracestuff"))
                        .and(hasEntry("mytraceheader2", "tracestuff2")));
        assertEquals("none",
                autoConfigured.getConfig().getString("otel.exporter.otlp.traces.compression"));
        assertEquals(2000,
                autoConfigured.getConfig().getInt("otel.exporter.otlp.traces.timeout"));
        assertEquals("none",
                autoConfigured.getConfig().getString("otel.metrics.exporter"));
    }
}
