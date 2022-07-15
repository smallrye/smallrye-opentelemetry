package io.smallrye.opentelemetry.implementation.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.extension.annotations.SpanAttribute;
import io.opentelemetry.extension.annotations.WithSpan;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.inject.ConfigExtension;
import io.smallrye.opentelemetry.implementation.cdi.OpenTelemetryExtension;
import io.smallrye.opentelemetry.implementation.common.InMemorySpanExporter;
import io.smallrye.opentelemetry.implementation.common.SpanBean;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.smallrye.opentelemetry.implementation.common.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;

@EnableAutoWeld
@AddExtensions({OpenTelemetryExtension.class, ConfigExtension.class})
class ConfigEnabledTest {

    public static SmallRyeConfig config;

    @BeforeAll
    static void beforeAll() {
        ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig());
        String[] properties = {"otel.experimental.sdk.enabled", "true",
                "otel.traces.exporter", "in-memory",
                "otel.metrics.exporter", "none"};
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
        spanExporter.assertSpanCount(1);
        OpenTelemetry sdk = spanExporter.getSDK();
        assertEquals("MultiTextMapPropagator",
                sdk.getPropagators().getTextMapPropagator().getClass().getSimpleName());
    }
}
