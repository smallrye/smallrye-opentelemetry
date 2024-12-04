package io.smallrye.opentelemetry.test;

import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static java.util.Comparator.comparingLong;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
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

    private static final List<String> KEY_COMPONENTS = List.of(HTTP_REQUEST_METHOD.getKey(),
            HTTP_ROUTE.getKey(),
            HTTP_RESPONSE_STATUS_CODE.getKey());

    @Inject
    InMemorySpanExporter spanExporter;
    @Inject
    InMemoryMetricExporter metricExporter;
    @Inject
    InMemoryLogRecordExporter logRecordExporter;

    public List<SpanData> getFinishedSpanItems(final int count) {
        assertSpanCount(count);
        return spanExporter.getFinishedSpanItems().stream()
                .sorted(comparingLong(SpanData::getStartEpochNanos)
                        .reversed())
                .collect(toList());
    }

    public void assertSpanCount(final int count) {
        await().atMost(5, SECONDS).untilAsserted(() -> assertEquals(count, spanExporter.getFinishedSpanItems().size()));
    }

    public MetricData getFinishedMetricItem(final String name) {
        assertMetricsAtLeast(1, name);
        return metrics(name).get(1);
    }

    public List<MetricData> getFinishedMetricItemList(final String name) {
        assertMetricsAtLeast(1, name);
        return metrics(name).metricData.collect(Collectors.toList());
    }

    public void assertMetricsAtLeast(final int count, final String name) {
        await().atMost(5, SECONDS).untilAsserted(() -> assertTrue(metrics(name).countDataPoints() >= count));
    }

    public void assertMetricsAtLeast(final int count, final String name, final String route) {
        await().atMost(5, SECONDS).untilAsserted(() -> assertTrue(metrics(name).route(route).countDataPoints() >= count,
                "Metrics count: " + metrics(name).route(route).countDataPoints() +
                        ". Found metrics " + metrics(name).route(route).metricData
                                .map(md -> md.getData().getPoints().stream()
                                        .map(pointData -> md.getName() + " : " + pointData.getAttributes().get(HTTP_ROUTE))
                                        .collect(toList()))
                                .collect(toList())));
    }

    public MetricData getFinishedHistogramItemByPoints(final String name, final int count) {
        await().atMost(5, SECONDS).untilAsserted(() -> assertEquals(count, histogram(name).countDataPoints()));
        return histogram(name).get(count);
    }

    public MetricData getLastFinishedHistogramItem(final String name, final int count) {
        await().atMost(5, SECONDS).untilAsserted(() -> assertTrue(histogram(name).getAll().stream().count() >= count));
        List<MetricData> metricDataList = histogram(name).metricData.collect(toList());
        return metricDataList.get(metricDataList.size() - 1); // last added entry
    }

    public MetricData getLastFinishedHistogramItem(final String name, final String route, final int count) {
        await().atMost(5, SECONDS)
                .untilAsserted(() -> assertTrue(histogram(name).route(route).getAll().stream().count() >= count));
        List<MetricData> metricDataList = histogram(name).route(route).metricData.collect(toList());
        return metricDataList.get(metricDataList.size() - 1); // last added entry
    }

    public void reset() {
        spanExporter.reset();
        metricExporter.reset();
        logRecordExporter.reset();
    }

    // Not for
    public Map<String, HistogramPointData> getMostRecentPointsMap(List<MetricData> finishedMetricItems) {
        // get last metric item
        Map<String, HistogramPointData> collect = finishedMetricItems.get(finishedMetricItems.size() - 1).getHistogramData()
                .getPoints().stream()
                .collect(toMap(
                        pointData -> pointData.getAttributes().asMap().entrySet().stream()
                                //valid attributes for the resulting map key
                                .filter(entry -> KEY_COMPONENTS.contains(entry.getKey().getKey()))
                                // ensure order
                                .sorted(Comparator.comparing(o -> o.getKey().getKey()))
                                // build key
                                .map(entry -> entry.getKey().getKey() + ":" + entry.getValue().toString())
                                .collect(joining(",")),
                        pointData -> pointData));
        return collect;
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

        MetricDataFilter path(final String path) {
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
                                            String value = pointData.getAttributes().get(URL_PATH);
                                            return value != null && value.equals(path);
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

        int countDataPoints() {
            return metricData.map(metricData1 -> countPoints(metricData1))
                    .mapToInt(Integer::intValue)
                    .sum();
        }

        MetricData get(final int count) {
            return metricData.filter(metricData -> countPoints(metricData) >= count)
                    .max(comparingLong(metricData -> metricData.getData().getPoints()
                            .stream()
                            .map(PointData::getEpochNanos)
                            .mapToLong(Long::longValue)
                            .max().orElse(0)))
                    .orElseThrow();
        }

        List<MetricData> getAll() {
            return metricData.collect(Collectors.toList());
        }

        int countPoints(final MetricData metricData) {
            return metricData.getData().getPoints().size();
        }
    }

    private MetricDataFilter metrics(final String name) {
        return new MetricDataFilter(name);
    }

    private MetricDataFilter histogram(final String name) {
        return new MetricDataFilter(name) {
            @Override
            int countPoints(final MetricData metricData) {
                return metricData.getData().getPoints().stream()
                        .map((o -> ((HistogramPointData) o).getCount()))
                        .mapToInt(Long::intValue)
                        .max().orElse(0);
            }
        };
    }
}
