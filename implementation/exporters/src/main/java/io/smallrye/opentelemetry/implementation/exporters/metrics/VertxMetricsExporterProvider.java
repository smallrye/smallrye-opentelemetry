package io.smallrye.opentelemetry.implementation.exporters.metrics;

import static io.smallrye.opentelemetry.implementation.exporters.Constants.PROTOCOL_GRPC;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.PROTOCOL_HTTP_PROTOBUF;
import static io.smallrye.opentelemetry.implementation.exporters.OtlpExporterUtil.getConfig;
import static io.smallrye.opentelemetry.implementation.exporters.OtlpExporterUtil.getProtocol;

import java.net.URISyntaxException;

import io.opentelemetry.exporter.internal.otlp.metrics.MetricsRequestMarshaler;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.DefaultAggregationSelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.internal.aggregator.AggregationUtil;
import io.smallrye.opentelemetry.implementation.exporters.AbstractVertxExporterProvider;
import io.smallrye.opentelemetry.implementation.exporters.sender.VertxGrpcSender;
import io.smallrye.opentelemetry.implementation.exporters.sender.VertxHttpSender;

public class VertxMetricsExporterProvider extends AbstractVertxExporterProvider<MetricsRequestMarshaler>
        implements ConfigurableMetricExporterProvider {

    public VertxMetricsExporterProvider() {
        super("metric", "otlp");
    }

    @Override
    public MetricExporter createExporter(ConfigProperties config) {
        try {
            final String protocol = getProtocol(config, getSignalType());

            if (PROTOCOL_GRPC.equals(protocol)) {
                return new VertxGrpcMetricExporter(
                        createGrpcExporter(config, VertxGrpcSender.GRPC_METRIC_SERVICE_NAME),
                        aggregationTemporalityResolver(config),
                        aggregationResolver(config));
            } else if (PROTOCOL_HTTP_PROTOBUF.equals(protocol)) {
                return new VertxHttpMetricsExporter(
                        createHttpExporter(config, VertxHttpSender.METRICS_PATH),
                        aggregationTemporalityResolver(config),
                        aggregationResolver(config));
            } else {
                throw buildUnsupportedProtocolException(protocol);
            }
        } catch (IllegalArgumentException | URISyntaxException iae) {
            throw new IllegalStateException("Unable to install OTLP Exporter", iae);
        }
    }

    private DefaultAggregationSelector aggregationResolver(ConfigProperties config) {
        String defaultHistogramAggregation = getConfig(config, "explicit_bucket_histogram",
                "otel.exporter.otlp.metrics.default.histogram.aggregation");

        DefaultAggregationSelector aggregationSelector;
        if (defaultHistogramAggregation.equals("explicit_bucket_histogram")) {
            aggregationSelector = DefaultAggregationSelector.getDefault();
        } else if (AggregationUtil.aggregationName(Aggregation.base2ExponentialBucketHistogram())
                .equalsIgnoreCase(defaultHistogramAggregation)) {
            aggregationSelector = DefaultAggregationSelector
                    .getDefault()
                    .with(InstrumentType.HISTOGRAM, Aggregation.base2ExponentialBucketHistogram());
        } else {
            throw new ConfigurationException(
                    "Unrecognized default histogram aggregation: " + defaultHistogramAggregation);
        }
        return aggregationSelector;
    }

    private AggregationTemporalitySelector aggregationTemporalityResolver(ConfigProperties config) {
        String temporalityValue = getConfig(config, "cumulative",
                "otel.exporter.otlp.metrics.temporality.preference");
        if (temporalityValue.equals("cumulative")) {
            return AggregationTemporalitySelector.alwaysCumulative();
        } else if (temporalityValue.equals("delta")) {
            return AggregationTemporalitySelector.deltaPreferred();
        } else if (temporalityValue.equals("lowmemory")) {
            return AggregationTemporalitySelector.lowMemory();
        } else {
            throw new ConfigurationException("Unrecognized aggregation temporality: " + temporalityValue);
        }
    }
}
