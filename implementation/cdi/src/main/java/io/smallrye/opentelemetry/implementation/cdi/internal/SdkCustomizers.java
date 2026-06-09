package io.smallrye.opentelemetry.implementation.cdi.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;

public class SdkCustomizers implements AutoConfigurationCustomizer {
    private final List<Supplier<Map<String, String>>> propertiesSuppliers = new ArrayList<>();
    private final List<Function<ConfigProperties, Map<String, String>>> propertiesCustomizers = new ArrayList<>();
    private final List<BiFunction<? super Resource, ConfigProperties, ? extends Resource>> resourceCustomizers = new ArrayList<>();
    private final List<BiFunction<? super TextMapPropagator, ConfigProperties, ? extends TextMapPropagator>> propagatorCustomizers = new ArrayList<>();
    private final List<BiFunction<? super Sampler, ConfigProperties, ? extends Sampler>> samplerCustomizers = new ArrayList<>();
    private final List<BiFunction<? super SpanExporter, ConfigProperties, ? extends SpanExporter>> spanExporterCustomizers = new ArrayList<>();
    private final List<BiFunction<? super SpanProcessor, ConfigProperties, ? extends SpanProcessor>> spanProcessorCustomizers = new ArrayList<>();
    private final List<BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder>> tracerProviderCustomizers = new ArrayList<>();
    private final List<BiFunction<? super MetricExporter, ConfigProperties, ? extends MetricExporter>> metricExporterCustomizers = new ArrayList<>();
    private final List<BiFunction<? super MetricReader, ConfigProperties, ? extends MetricReader>> metricReaderCustomizers = new ArrayList<>();
    private final List<BiFunction<SdkMeterProviderBuilder, ConfigProperties, SdkMeterProviderBuilder>> meterProviderCustomizers = new ArrayList<>();
    private final List<BiFunction<? super LogRecordExporter, ConfigProperties, ? extends LogRecordExporter>> logRecordExporterCustomizers = new ArrayList<>();
    private final List<BiFunction<? super LogRecordProcessor, ConfigProperties, ? extends LogRecordProcessor>> logRecordProcessorCustomizers = new ArrayList<>();
    private final List<BiFunction<SdkLoggerProviderBuilder, ConfigProperties, SdkLoggerProviderBuilder>> loggerProviderCustomizers = new ArrayList<>();

    public List<Supplier<Map<String, String>>> getPropertiesSuppliers() {
        return propertiesSuppliers;
    }

    public List<Function<ConfigProperties, Map<String, String>>> getPropertiesCustomizers() {
        return propertiesCustomizers;
    }

    public List<BiFunction<? super Resource, ConfigProperties, ? extends Resource>> getResourceCustomizers() {
        return resourceCustomizers;
    }

    public List<BiFunction<? super TextMapPropagator, ConfigProperties, ? extends TextMapPropagator>> getPropagatorCustomizers() {
        return propagatorCustomizers;
    }

    public List<BiFunction<? super Sampler, ConfigProperties, ? extends Sampler>> getSamplerCustomizers() {
        return samplerCustomizers;
    }

    public List<BiFunction<? super SpanExporter, ConfigProperties, ? extends SpanExporter>> getSpanExporterCustomizers() {
        return spanExporterCustomizers;
    }

    public List<BiFunction<? super SpanProcessor, ConfigProperties, ? extends SpanProcessor>> getSpanProcessorCustomizers() {
        return spanProcessorCustomizers;
    }

    public List<BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder>> getTracerProviderCustomizers() {
        return tracerProviderCustomizers;
    }

    public List<BiFunction<? super MetricExporter, ConfigProperties, ? extends MetricExporter>> getMetricExporterCustomizers() {
        return metricExporterCustomizers;
    }

    public List<BiFunction<? super MetricReader, ConfigProperties, ? extends MetricReader>> getMetricReaderCustomizers() {
        return metricReaderCustomizers;
    }

    public List<BiFunction<SdkMeterProviderBuilder, ConfigProperties, SdkMeterProviderBuilder>> getMeterProviderCustomizers() {
        return meterProviderCustomizers;
    }

    public List<BiFunction<? super LogRecordExporter, ConfigProperties, ? extends LogRecordExporter>> getLogRecordExporterCustomizers() {
        return logRecordExporterCustomizers;
    }

    public List<BiFunction<? super LogRecordProcessor, ConfigProperties, ? extends LogRecordProcessor>> getLogRecordProcessorCustomizers() {
        return logRecordProcessorCustomizers;
    }

    public List<BiFunction<SdkLoggerProviderBuilder, ConfigProperties, SdkLoggerProviderBuilder>> getLoggerProviderCustomizers() {
        return loggerProviderCustomizers;
    }

    @Override
    public AutoConfigurationCustomizer addPropertiesSupplier(Supplier<Map<String, String>> propertiesSupplier) {
        propertiesSuppliers.add(propertiesSupplier);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addPropertiesCustomizer(
            Function<ConfigProperties, Map<String, String>> propertiesCustomizer) {
        propertiesCustomizers.add(propertiesCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addResourceCustomizer(
            BiFunction<? super Resource, ConfigProperties, ? extends Resource> resourceCustomizer) {
        resourceCustomizers.add(resourceCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addPropagatorCustomizer(
            BiFunction<? super TextMapPropagator, ConfigProperties, ? extends TextMapPropagator> propagatorCustomizer) {
        propagatorCustomizers.add(propagatorCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addSamplerCustomizer(
            BiFunction<? super Sampler, ConfigProperties, ? extends Sampler> samplerCustomizer) {
        samplerCustomizers.add(samplerCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addSpanExporterCustomizer(
            BiFunction<? super SpanExporter, ConfigProperties, ? extends SpanExporter> exporterCustomizer) {
        spanExporterCustomizers.add(exporterCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addSpanProcessorCustomizer(
            BiFunction<? super SpanProcessor, ConfigProperties, ? extends SpanProcessor> spanProcessorCustomizer) {
        spanProcessorCustomizers.add(spanProcessorCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addTracerProviderCustomizer(
            BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder> tracerProviderCustomizer) {
        tracerProviderCustomizers.add(tracerProviderCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addMetricExporterCustomizer(
            BiFunction<? super MetricExporter, ConfigProperties, ? extends MetricExporter> exporterCustomizer) {
        metricExporterCustomizers.add(exporterCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addMetricReaderCustomizer(
            BiFunction<? super MetricReader, ConfigProperties, ? extends MetricReader> readerCustomizer) {
        metricReaderCustomizers.add(readerCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addMeterProviderCustomizer(
            BiFunction<SdkMeterProviderBuilder, ConfigProperties, SdkMeterProviderBuilder> meterProviderCustomizer) {
        meterProviderCustomizers.add(meterProviderCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addLogRecordExporterCustomizer(
            BiFunction<? super LogRecordExporter, ConfigProperties, ? extends LogRecordExporter> exporterCustomizer) {
        logRecordExporterCustomizers.add(exporterCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addLogRecordProcessorCustomizer(
            BiFunction<? super LogRecordProcessor, ConfigProperties, ? extends LogRecordProcessor> logRecordProcessorCustomizer) {
        logRecordProcessorCustomizers.add(logRecordProcessorCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addLoggerProviderCustomizer(
            BiFunction<SdkLoggerProviderBuilder, ConfigProperties, SdkLoggerProviderBuilder> loggerProviderCustomizer) {
        loggerProviderCustomizers.add(loggerProviderCustomizer);
        return this;
    }
}
