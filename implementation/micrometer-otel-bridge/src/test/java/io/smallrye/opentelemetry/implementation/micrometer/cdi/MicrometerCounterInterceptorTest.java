package io.smallrye.opentelemetry.implementation.micrometer.cdi;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.metrics.data.MetricData;
import jakarta.inject.Inject;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.config.inject.ConfigExtension;
import io.smallrye.opentelemetry.implementation.cdi.OpenTelemetryExtension;
import io.smallrye.opentelemetry.implementation.config.OpenTelemetryConfigProducer;

import java.util.List;

@EnableAutoWeld
@AddExtensions({ OpenTelemetryExtension.class, ConfigExtension.class, MicrometerExtension.class })
@AddBeanClasses({ OpenTelemetryConfigProducer.class, InMemoryMetricExporter.class, InMemoryMetricExporterProvider.class,
        InMemorySpanExporter.class, InMemorySpanExporterProvider.class,
        CountedResource.class, TestValueResolver.class })
public class MicrometerCounterInterceptorTest {

    @Inject
    MeterRegistry registry;

    @Inject
    CountedResource counted;

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    InMemoryMetricExporter metricExporter;

    @Test
    void testCountAllMetrics_MetricsOnFailure() {
        Assertions.assertThrows(NullPointerException.class, () -> counted.countAllInvocations(true));
        Counter counter = registry.get("metric.all")
                .tag("method", "countAllInvocations")
                .tag("class", "io.smallrye.opentelemetry.implementation.micrometer.cdi.CountedResource")
                .tag("extra", "tag")
//                .tag("do_fail", "prefix_true") // FIXME @MeterTag not implemented yet
                .tag("exception", "NullPointerException")
                .tag("result", "failure").counter();
        Assertions.assertNotNull(counter);
//        Assertions.assertEquals(1, counter.count()); // FIXME counter.count() not implemented!!!!!

//        openTelemetry.getMeter("metric.all")
        metricExporter.assertCount(1);
        List<MetricData> finishedMetricItems = metricExporter.getFinishedMetricItems();


        Assertions.assertNull(counter.getId().getDescription());
    }
}
