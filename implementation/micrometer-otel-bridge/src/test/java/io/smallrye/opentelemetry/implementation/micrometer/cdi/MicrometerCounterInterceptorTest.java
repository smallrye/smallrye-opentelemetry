package io.smallrye.opentelemetry.implementation.micrometer.cdi;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.attributeEntry;
import static org.assertj.core.api.ListAssert.assertThatList;

import java.util.List;

import jakarta.inject.Inject;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.smallrye.config.inject.ConfigExtension;
import io.smallrye.opentelemetry.implementation.cdi.OpenTelemetryExtension;
import io.smallrye.opentelemetry.implementation.config.OpenTelemetryConfigProducer;
import io.smallrye.opentelemetry.test.InMemoryMetricExporter;
import io.smallrye.opentelemetry.test.InMemoryMetricExporterProvider;
import io.smallrye.opentelemetry.test.InMemorySpanExporter;
import io.smallrye.opentelemetry.test.InMemorySpanExporterProvider;

@EnableAutoWeld
@AddExtensions({ OpenTelemetryExtension.class, ConfigExtension.class, MicrometerExtension.class })
@AddBeanClasses({ OpenTelemetryConfigProducer.class, InMemoryMetricExporter.class, InMemoryMetricExporterProvider.class,
        InMemorySpanExporter.class, InMemorySpanExporterProvider.class,
        CountedBean.class, TestValueResolver.class })
public class MicrometerCounterInterceptorTest {

    @Inject
    CountedBean countedBean;

    @Inject
    MeterRegistry registry;

    @Inject
    InMemoryMetricExporter metricExporter;

    @BeforeEach
    void setup() {
        metricExporter.reset();
    }

    @Test
    void testCountAllMetrics_MetricsOnSuccess() {
        countedBean.countAllInvocations(false);

        Counter counter = registry.get("metric.all")
                .tag("method", "countAllInvocations")
                .tag("class", "io.smallrye.opentelemetry.implementation.micrometer.cdi.CountedBean")
                .tag("extra", "tag")
                //                .tag("do_fail", "prefix_true") // FIXME @MeterTag not implemented yet
                .tag("result", "success").counter();
        Assertions.assertNotNull(counter);

        metricExporter.assertCountAtLeast(1);
        List<MetricData> finishedMetricItems = metricExporter.getFinishedMetricItems("metric.all", null);
        assertThatList(finishedMetricItems)
                .isNotEmpty()
                .anySatisfy(metricData -> assertThat(metricData)
                        .isNotNull()
                        .hasName("metric.all")
                        .hasDescription("Total number of invocations for method")
                        .hasUnit("invocations")
                        .hasDoubleSumSatisfying(sum -> sum.hasPointsSatisfying(point -> point.hasValue(1)
                                .hasAttributes(attributeEntry(
                                        "class",
                                        "io.smallrye.opentelemetry.implementation.micrometer.cdi.CountedBean"),
                                        attributeEntry("method", "countAllInvocations"),
                                        attributeEntry("extra", "tag"),
                                        attributeEntry("exception", "none"),
                                        attributeEntry("result", "success")))));
    }

    @Test
    void testCountAllMetrics_MetricsOnFailure() {
        Assertions.assertThrows(NullPointerException.class, () -> countedBean.countAllInvocations(true));

        Counter counter = registry.get("metric.all")
                .tag("method", "countAllInvocations")
                .tag("class", "io.smallrye.opentelemetry.implementation.micrometer.cdi.CountedBean")
                .tag("extra", "tag")
                //                .tag("do_fail", "prefix_true") // FIXME @MeterTag not implemented yet
                .tag("exception", "NullPointerException")
                .tag("result", "failure").counter();
        Assertions.assertNotNull(counter);

        metricExporter.assertCountAtLeast(1);
        List<MetricData> finishedMetricItems = metricExporter.getFinishedMetricItems("metric.all", null);
        assertThatList(finishedMetricItems)
                .isNotEmpty()
                .anySatisfy(metricData -> assertThat(metricData)
                        .isNotNull()
                        .hasName("metric.all")
                        .hasDescription("Total number of invocations for method")
                        .hasUnit("invocations")
                        .hasDoubleSumSatisfying(sum -> sum.hasPointsSatisfying(point -> point.hasValue(1)
                                .hasAttributes(attributeEntry(
                                        "class",
                                        "io.smallrye.opentelemetry.implementation.micrometer.cdi.CountedBean"),
                                        attributeEntry("method", "countAllInvocations"),
                                        attributeEntry("extra", "tag"),
                                        attributeEntry("exception", "NullPointerException"),
                                        attributeEntry("result", "failure")))));
    }
}
