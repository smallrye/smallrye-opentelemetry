package io.smallrye.opentelemetry.implementation.micrometer.cdi;

import static io.smallrye.common.constraint.Assert.assertNotNull;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.config.inject.ConfigExtension;
import io.smallrye.opentelemetry.implementation.cdi.OpenTelemetryExtension;
import io.smallrye.opentelemetry.implementation.config.OpenTelemetryConfigProducer;

@EnableAutoWeld
@AddExtensions({ OpenTelemetryExtension.class, ConfigExtension.class, MicrometerExtension.class })
@AddBeanClasses(OpenTelemetryConfigProducer.class)
class MeterRegistryProducerTest {

    @Inject
    TestBean testBean;

    @Test
    void testRegistryInjection() {
        assertNotNull(testBean);
    }

    @ApplicationScoped
    public static class TestBean {
        @Inject
        MeterRegistry registry;

        public void getRegistry() {
            registry.counter("getRegistry").increment();
        }
    }
}
