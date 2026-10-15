package io.smallrye.opentelemetry.observation.test.metrics.cdi;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.smallrye.opentelemetry.test.InMemoryExporter;

@ExtendWith(ArquillianExtension.class)
public class GaugeCdiTest {
    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class);
    }

    @Inject
    MeterBean meterBean;

    @Inject
    InMemoryExporter exporter;

    @BeforeEach
    void setUp() {
        exporter.reset();
    }

    @Test
    void gauge() throws InterruptedException {
        meterBean.getMeter()
                .gaugeBuilder("jvm.memory.total")
                .setDescription("Reports JVM memory usage. -> Manual")
                .setUnit("byte")
                .buildWithCallback(
                        result -> result.record(Runtime.getRuntime().totalMemory(), Attributes.empty()));
        exporter.assertMetricsAtLeast(1, "jvm.memory.total");
    }

    @Test
    void meter() {
        assertNotNull(meterBean.getMeter());
    }

    @ApplicationScoped
    public static class MeterBean {
        @Inject
        Meter meter;

        public Meter getMeter() {
            return meter;
        }
    }
}
