package io.smallrye.opentelemetry.implementation.micrometer.cdi;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.attributeEntry;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.smallrye.config.inject.ConfigExtension;
import io.smallrye.opentelemetry.implementation.cdi.OpenTelemetryExtension;
import io.smallrye.opentelemetry.implementation.config.OpenTelemetryConfigProducer;
import io.smallrye.opentelemetry.test.InMemoryExporter;
import io.smallrye.opentelemetry.test.InMemoryExporterProducer;

@EnableAutoWeld
@AddExtensions({ OpenTelemetryExtension.class, ConfigExtension.class, MicrometerExtension.class })
@AddBeanClasses({ OpenTelemetryConfigProducer.class, InMemoryExporterProducer.class })
public class HistogramTest {

    @Inject
    ManualHistogramBean manualHistogramBean;
    @Inject
    InMemoryExporter exporter;

    @Test
    void histogramTest() {
        manualHistogramBean.recordHistogram();

        MetricData testSummary = exporter.getFinishedHistogramItem("testSummary", 4);
        assertNotNull(testSummary);
        assertThat(testSummary)
                .hasDescription("This is a test distribution summary")
                .hasUnit("things")
                .hasHistogramSatisfying(
                        histogram -> histogram.hasPointsSatisfying(
                                points -> points
                                        .hasSum(555.5)
                                        .hasCount(4)
                                        .hasAttributes(attributeEntry("tag", "value"))));

        MetricData textSummaryMax = exporter.getFinishedMetricItem("testSummary.max");
        assertNotNull(textSummaryMax);
        assertThat(textSummaryMax)
                .hasDescription("This is a test distribution summary")
                .hasDoubleGaugeSatisfying(
                        gauge -> gauge.hasPointsSatisfying(
                                point -> point
                                        .hasValue(500)
                                        .hasAttributes(attributeEntry("tag", "value"))));

        MetricData testSummaryHistogram = exporter.getFinishedMetricItem("testSummary.histogram");
        assertNotNull(testSummaryHistogram);
        assertThat(testSummaryHistogram)
                .hasDoubleGaugeSatisfying(
                        gauge -> gauge.hasPointsSatisfying(
                                point -> point
                                        .hasValue(1)
                                        .hasAttributes(
                                                attributeEntry("le", "1"),
                                                attributeEntry("tag", "value")),
                                point -> point
                                        .hasValue(2)
                                        .hasAttributes(
                                                attributeEntry("le", "10"),
                                                attributeEntry("tag", "value")),
                                point -> point
                                        .hasValue(3)
                                        .hasAttributes(
                                                attributeEntry("le", "100"),
                                                attributeEntry("tag", "value")),
                                point -> point
                                        .hasValue(4)
                                        .hasAttributes(
                                                attributeEntry("le", "1000"),
                                                attributeEntry("tag", "value"))));
    }

    @ApplicationScoped
    public static class ManualHistogramBean {
        @Inject
        MeterRegistry registry;

        public void recordHistogram() {
            DistributionSummary summary = DistributionSummary.builder("testSummary")
                    .description("This is a test distribution summary")
                    .baseUnit("things")
                    .tags("tag", "value")
                    .serviceLevelObjectives(1, 10, 100, 1000)
                    .distributionStatisticBufferLength(10)
                    .register(registry);

            summary.record(0.5);
            summary.record(5);
            summary.record(50);
            summary.record(500);
        }
    }
}
