package io.smallrye.opentelemetry.extra.test.metrics.cdi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.PointData;
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
    void gauge() {
        meterBean.getMeter()
                .gaugeBuilder("jvm.memory.total")
                .setDescription("Reports JVM memory usage.")
                .setUnit("byte")
                .buildWithCallback(result -> result.record(Runtime.getRuntime().totalMemory(), Attributes.empty()));

        MetricData metricData = exporter.getFinishedMetricItem("jvm.memory.total");
        assertEquals("Reports JVM memory usage.", metricData.getDescription());
        assertEquals("io.smallrye.opentelemetry", metricData.getInstrumentationScopeInfo().getName());
        assertFalse(metricData.getData().getPoints().isEmpty());
        for (PointData pointData : metricData.getData().getPoints()) {
            assertTrue(pointData.getStartEpochNanos() > 0);
            assertTrue(pointData.getEpochNanos() > 0);
            assertTrue(pointData instanceof DoublePointData);
            DoublePointData doublePointData = (DoublePointData) pointData;
            assertTrue(doublePointData.getValue() > 0);
        }
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
