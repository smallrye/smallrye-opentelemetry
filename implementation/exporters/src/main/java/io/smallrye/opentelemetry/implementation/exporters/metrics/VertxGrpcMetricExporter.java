package io.smallrye.opentelemetry.implementation.exporters.metrics;

import java.util.Collection;

import io.opentelemetry.exporter.internal.otlp.metrics.MetricsRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.GrpcSender;
import io.opentelemetry.sdk.common.export.GrpcStatusCode;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.DefaultAggregationSelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

public class VertxGrpcMetricExporter implements MetricExporter {

    private final GrpcSender sender;
    private final AggregationTemporalitySelector aggregationTemporalitySelector;
    private final DefaultAggregationSelector defaultAggregationSelector;

    public VertxGrpcMetricExporter(GrpcSender sender,
            AggregationTemporalitySelector aggregationTemporalitySelector,
            DefaultAggregationSelector defaultAggregationSelector) {
        this.sender = sender;
        this.aggregationTemporalitySelector = aggregationTemporalitySelector;
        this.defaultAggregationSelector = defaultAggregationSelector;
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        MetricsRequestMarshaler marshaler = MetricsRequestMarshaler.create(metrics);
        CompletableResultCode result = new CompletableResultCode();
        sender.send(marshaler.toBinaryMessageWriter(),
                response -> {
                    if (response.getStatusCode() == GrpcStatusCode.OK) {
                        result.succeed();
                    } else {
                        result.fail();
                    }
                },
                throwable -> result.fail());
        return result;
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return sender.shutdown();
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return this.aggregationTemporalitySelector.getAggregationTemporality(instrumentType);
    }

    @Override
    public Aggregation getDefaultAggregation(InstrumentType instrumentType) {
        return defaultAggregationSelector.getDefaultAggregation(instrumentType);
    }

    @Override
    public MemoryMode getMemoryMode() {
        return MemoryMode.IMMUTABLE_DATA;
    }
}
