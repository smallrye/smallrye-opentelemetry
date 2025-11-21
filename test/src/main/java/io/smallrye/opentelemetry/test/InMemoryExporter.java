package io.smallrye.opentelemetry.test;

import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE;
import static java.util.Comparator.comparingLong;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.metrics.data.Data;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;

@ApplicationScoped
public class InMemoryExporter {
    @Inject
    InMemorySpanExporter spanExporter;
    @Inject
    InMemoryMetricExporter metricExporter;
    @Inject
    InMemoryLogRecordExporter logRecordExporter;

    public List<SpanData> getFinishedSpanItems(final int count) {
        assertSpanCount(count);
        return spanExporter.getFinishedSpanItems().stream().sorted(comparingLong(SpanData::getStartEpochNanos).reversed())
                .collect(toList());
    }

    public void assertSpanCount(final int count) {
        await().atMost(5, SECONDS).untilAsserted(() -> assertEquals(count, spanExporter.getFinishedSpanItems().size()));
    }

    public MetricData getFinishedMetricItem(final String name) {
        assertMetricsAtLeast(1, name);
        return metrics(name).get(1);
    }

    public MetricData getFinishedMetricItem(final String name, final String route) {
        assertMetricsAtLeast(1, name, route);
        return metrics(name).route(route).get(1);
    }

    public void assertMetricsAtLeast(final int count, final String name) {
        await().atMost(5, SECONDS).untilAsserted(() -> assertTrue(metrics(name).count() >= count));
    }

    public void assertMetricsAtLeast(final int count, final String name, final String route) {
        await().atMost(5, SECONDS).untilAsserted(() -> assertTrue(metrics(name).route(route).count() >= count));
    }

    public MetricData getFinishedHistogramItem(final String name, final int count) {
        await().atMost(5, SECONDS).untilAsserted(() -> assertEquals(count, histogram(name).count()));
        return histogram(name).get(count);
    }

    public MetricData getFinishedHistogramItem(final String name, final String route, final int count) {
        await().atMost(5, SECONDS).untilAsserted(() -> assertEquals(count, histogram(name).route(route).count()));
        return histogram(name).route(route).get(count);
    }

    public List<LogRecordData> getFinishedLogRecordItems(final int count) {
        assertSpanCount(count);
        return logRecordExporter.getFinishedLogRecordItems();
    }

    public void reset() {
        spanExporter.reset();
        metricExporter.reset();
        logRecordExporter.reset();
    }

    private class MetricDataFilter {
        private Stream<MetricData> metricData;

        MetricDataFilter(final String name) {
            metricData = metricExporter.getFinishedMetricItems()
                    .stream()
                    .filter(metricData -> metricData.getName().equals(name));
        }

        MetricDataFilter route(final String route) {
            metricData = metricData.map(new Function<MetricData, MetricData>() {
                @Override
                public MetricData apply(final MetricData metricData) {
                    return new MetricData() {
                        @Override
                        public Resource getResource() {
                            return metricData.getResource();
                        }

                        @Override
                        public InstrumentationScopeInfo getInstrumentationScopeInfo() {
                            return metricData.getInstrumentationScopeInfo();
                        }

                        @Override
                        public String getName() {
                            return metricData.getName();
                        }

                        @Override
                        public String getDescription() {
                            return metricData.getDescription();
                        }

                        @Override
                        public String getUnit() {
                            return metricData.getUnit();
                        }

                        @Override
                        public MetricDataType getType() {
                            return metricData.getType();
                        }

                        @Override
                        public Data<?> getData() {
                            return new Data<PointData>() {
                                @Override
                                public Collection<PointData> getPoints() {
                                    return metricData.getData().getPoints().stream().filter(new Predicate<PointData>() {
                                        @Override
                                        public boolean test(final PointData pointData) {
                                            String value = pointData.getAttributes().get(HTTP_ROUTE);
                                            return value != null && value.equals(route);
                                        }
                                    }).collect(Collectors.toSet());
                                }
                            };
                        }
                    };
                }
            });
            return this;
        }

        int count() {
            return metricData.map(this::count)
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0);
        }

        MetricData get(final int count) {
            return metricData.filter(metricData -> count(metricData) >= count)
                    .max(comparingLong(metricData -> metricData.getData().getPoints()
                            .stream()
                            .map(PointData::getEpochNanos)
                            .mapToLong(Long::longValue)
                            .max().orElse(0)))
                    .orElseThrow();
        }

        int count(final MetricData metricData) {
            return metricData.getData().getPoints().size();
        }
    }

    private MetricDataFilter metrics(final String name) {
        return new MetricDataFilter(name);
    }

    private MetricDataFilter histogram(final String name) {
        return new MetricDataFilter(name) {
            @Override
            int count(final MetricData metricData) {
                return metricData.getData().getPoints().stream()
                        .map((o -> ((HistogramPointData) o).getCount()))
                        .mapToInt(Long::intValue)
                        .max().orElse(0);
            }
        };
    }
}
