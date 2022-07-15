package io.smallrye.opentelemetry.implementation.config;

import static io.smallrye.opentelemetry.implementation.common.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.inject.ConfigExtension;
import io.smallrye.opentelemetry.implementation.cdi.OpenTelemetryExtension;
import io.smallrye.opentelemetry.implementation.common.InMemorySpanExporter;
import io.smallrye.opentelemetry.implementation.common.SpanBean;

@EnableAutoWeld
@AddExtensions({ OpenTelemetryExtension.class, ConfigExtension.class })
class ConfigDisabledTest {
    public static SmallRyeConfig config;

    @BeforeAll
    static void beforeAll() {
        ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig());
        config = new SmallRyeConfigBuilder()
                .withSources(config("otel.experimental.sdk.enabled", "false",
                        "otel.traces.exporter", "in-memory",
                        "otel.metrics.exporter", "none"))
                .addDefaultInterceptors()
                .build();
        ConfigProviderResolver.instance().registerConfig(config, Thread.currentThread().getContextClassLoader());
    }

    @AfterAll
    static void afterAll() {
        ConfigProviderResolver.instance().releaseConfig(config);
    }

    @Inject
    InMemorySpanExporter spanExporter;
    @Inject
    SpanBean spanBean;

    @BeforeEach
    void setUp() {
        spanExporter.reset();
        GlobalOpenTelemetry.resetForTest();
    }

    /**
     * Whe the sdk is disabled Providers and Propagators are set no NOOP.
     */
    @Test
    void span() {
        spanBean.span();
        OpenTelemetry sdk = spanExporter.getSDK();
        assertEquals("NoopTextMapPropagator",
                sdk.getPropagators().getTextMapPropagator().getClass().getSimpleName());
    }
}
