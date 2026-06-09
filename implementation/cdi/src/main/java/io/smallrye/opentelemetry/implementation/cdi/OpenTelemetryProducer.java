package io.smallrye.opentelemetry.implementation.cdi;

import static io.smallrye.opentelemetry.api.OpenTelemetryConfig.INSTRUMENTATION_NAME;
import static io.smallrye.opentelemetry.api.OpenTelemetryConfig.INSTRUMENTATION_VERSION;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Singleton;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.api.baggage.BaggageEntry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.instrumentation.runtimemetrics.java8.RuntimeMetrics;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.smallrye.opentelemetry.api.OpenTelemetryConfig;
import io.smallrye.opentelemetry.api.OpenTelemetryLogHandler;
import io.smallrye.opentelemetry.implementation.cdi.internal.SdkCustomizers;

@Singleton
public class OpenTelemetryProducer {
    private final List<AutoCloseable> closeables = new ArrayList<>();
    ConfigProperties configProperties;

    @Produces
    @Singleton
    public OpenTelemetry getOpenTelemetry(final OpenTelemetryConfig config) {
        Map<String, String> originalProps = config.properties();

        if ("true".equalsIgnoreCase(originalProps.get("otel.sdk.disabled"))) {
            return OpenTelemetry.noop();
        }

        configProperties = DefaultConfigProperties.createFromMap(originalProps);

        return performPrivileged(() -> {
            var otel = buildSdk(originalProps);
            closeables.add(RuntimeMetrics.create(otel));
            OpenTelemetryLogHandler.install(otel);
            return otel;
        });
    }

    @Produces
    @Singleton
    public Tracer getTracer() {
        return CDI.current().select(OpenTelemetry.class).get().getTracer(INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION);
    }

    @Produces
    @Singleton
    public Meter getMeter() {
        return CDI.current().select(OpenTelemetry.class).get().getMeter(INSTRUMENTATION_NAME);
    }

    @Produces
    @RequestScoped
    @SuppressWarnings("NullableProblems")
    public Span getSpan() {
        return new Span() {
            @Override
            public <T> Span setAttribute(final AttributeKey<T> key, final T value) {
                return Span.current().setAttribute(key, value);
            }

            @Override
            public Span addEvent(final String name, final Attributes attributes) {
                return Span.current().addEvent(name, attributes);
            }

            @Override
            public Span addEvent(final String name, final Attributes attributes, final long timestamp,
                    final TimeUnit unit) {
                return Span.current().addEvent(name, attributes, timestamp, unit);
            }

            @Override
            public Span setStatus(final StatusCode statusCode, final String description) {
                return Span.current().setStatus(statusCode, description);
            }

            @Override
            public Span recordException(final Throwable exception, final Attributes additionalAttributes) {
                return Span.current().recordException(exception, additionalAttributes);
            }

            @Override
            public Span updateName(final String name) {
                return Span.current().updateName(name);
            }

            @Override
            public void end() {
                Span.current().end();
            }

            @Override
            public void end(final long timestamp, final TimeUnit unit) {
                Span.current().end(timestamp, unit);
            }

            @Override
            public SpanContext getSpanContext() {
                return Span.current().getSpanContext();
            }

            @Override
            public boolean isRecording() {
                return Span.current().isRecording();
            }
        };
    }

    @Produces
    @RequestScoped
    @SuppressWarnings("NullableProblems")
    public Baggage getBaggage() {
        return new Baggage() {
            @Override
            public int size() {
                return Baggage.current().size();
            }

            @Override
            public void forEach(final java.util.function.BiConsumer<? super String, ? super BaggageEntry> consumer) {
                Baggage.current().forEach(consumer);
            }

            @Override
            public Map<String, BaggageEntry> asMap() {
                return Baggage.current().asMap();
            }

            @Override
            public String getEntryValue(final String entryKey) {
                return Baggage.current().getEntryValue(entryKey);
            }

            @Override
            public BaggageBuilder toBuilder() {
                return Baggage.current().toBuilder();
            }
        };
    }

    void close(@Disposes final OpenTelemetry openTelemetry) throws Exception {
        for (AutoCloseable closeable : closeables) {
            closeable.close();
        }

        if (openTelemetry instanceof OpenTelemetrySdk openTelemetrySdk) {
            List<CompletableResultCode> shutdown = new ArrayList<>();
            shutdown.add(openTelemetrySdk.getSdkTracerProvider().shutdown());
            shutdown.add(openTelemetrySdk.getSdkMeterProvider().shutdown());
            shutdown.add(openTelemetrySdk.getSdkLoggerProvider().shutdown());
            CompletableResultCode.ofAll(shutdown).join(10, TimeUnit.SECONDS);
        }

    }

    public static <T> T performPrivileged(PrivilegedAction<T> action) {
        if (System.getSecurityManager() == null) {
            return action.run();
        } else {
            return AccessController.doPrivileged(action);
        }
    }

    private OpenTelemetrySdk buildSdk(Map<String, String> originalProps) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        SdkCustomizers customizers = new SdkCustomizers();
        for (AutoConfigurationCustomizerProvider provider : ServiceLoader.load(AutoConfigurationCustomizerProvider.class,
                tccl)) {
            provider.customize(customizers);
        }

        // Apply properties suppliers (lower priority than original config)
        Map<String, String> mergedProps = new HashMap<>();
        for (Supplier<Map<String, String>> supplier : customizers.getPropertiesSuppliers()) {
            Map<String, String> supplied = supplier.get();
            if (supplied != null) {
                mergedProps.putAll(supplied);
            }
        }
        mergedProps.putAll(originalProps);
        configProperties = DefaultConfigProperties.createFromMap(mergedProps);

        // Apply properties customizers (highest priority, can override anything)
        for (Function<ConfigProperties, Map<String, String>> fn : customizers.getPropertiesCustomizers()) {
            Map<String, String> extra = fn.apply(configProperties);
            if (extra != null && !extra.isEmpty()) {
                mergedProps.putAll(extra);
                configProperties = DefaultConfigProperties.createFromMap(mergedProps);
            }
        }

        Resource resource = buildResource(customizers, tccl);

        return OpenTelemetrySdk.builder()
                .setTracerProvider(buildTracerProvider(resource, customizers, tccl))
                .setMeterProvider(buildMeterProvider(resource, customizers))
                .setLoggerProvider(buildLoggerProvider(resource, customizers))
                .setPropagators(buildPropagators(customizers, tccl))
                .build();
    }

    private Resource buildResource(SdkCustomizers customizers, ClassLoader tccl) {
        Resource resource = Resource.getDefault();

        AttributesBuilder attrs = Attributes.builder();
        attrs.put(AttributeKey.stringKey("service.name"), configProperties.getString("otel.service.name"));

        String resourceAttributes = configProperties.getString("otel.resource.attributes");
        if (resourceAttributes != null) {
            for (String pair : resourceAttributes.split(",")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    attrs.put(AttributeKey.stringKey(kv[0].trim()), kv[1].trim());
                }
            }
        }

        resource = resource.merge(Resource.create(attrs.build()));

        for (ResourceProvider provider : ServiceLoader.load(ResourceProvider.class, tccl)) {
            resource = resource.merge(provider.createResource(configProperties));
        }

        for (BiFunction<? super Resource, ConfigProperties, ? extends Resource> fn : customizers.getResourceCustomizers()) {
            resource = fn.apply(resource, configProperties);
        }

        return resource;
    }

    private SdkTracerProvider buildTracerProvider(Resource resource, SdkCustomizers customizers, ClassLoader tccl) {
        Sampler sampler = loadSampler(tccl);
        for (BiFunction<? super Sampler, ConfigProperties, ? extends Sampler> fn : customizers.getSamplerCustomizers()) {
            sampler = fn.apply(sampler, configProperties);
        }

        SdkTracerProviderBuilder builder = SdkTracerProvider.builder()
                .setResource(resource)
                .setSampler(sampler);

        String exporterName = configProperties.getString("otel.traces.exporter", "otlp");
        if (!"none".equals(exporterName)) {
            SpanExporter exporter = loadSpanExporter(exporterName, tccl);
            if (exporter != null) {
                for (BiFunction<? super SpanExporter, ConfigProperties, ? extends SpanExporter> fn : customizers
                        .getSpanExporterCustomizers()) {
                    exporter = fn.apply(exporter, configProperties);
                }
                SpanProcessor processor;
                if (System.getSecurityManager() == null) {
                    processor = SimpleSpanProcessor.create(exporter);
                } else {
                    processor = BatchSpanProcessor.builder(exporter)
                            .setScheduleDelay(configProperties.getLong("otel.bsp.schedule.delay", 5000),
                                    TimeUnit.MILLISECONDS)
                            .build();
                }
                for (BiFunction<? super SpanProcessor, ConfigProperties, ? extends SpanProcessor> fn : customizers
                        .getSpanProcessorCustomizers()) {
                    processor = fn.apply(processor, configProperties);
                }
                builder.addSpanProcessor(processor);
            }
        }

        for (BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder> fn : customizers
                .getTracerProviderCustomizers()) {
            builder = fn.apply(builder, configProperties);
        }

        return builder.build();
    }

    private Sampler loadSampler(ClassLoader tccl) {
        String samplerName = configProperties.getString("otel.traces.sampler", "parentbased_always_on");
        return switch (samplerName) {
            case "always_on" -> Sampler.alwaysOn();
            case "always_off" -> Sampler.alwaysOff();
            case "traceidratio" ->
                Sampler.traceIdRatioBased(configProperties.getDouble("otel.traces.sampler.arg", 1.0));
            case "parentbased_always_on" -> Sampler.parentBased(Sampler.alwaysOn());
            case "parentbased_always_off" -> Sampler.parentBased(Sampler.alwaysOff());
            case "parentbased_traceidratio" ->
                Sampler.parentBased(Sampler.traceIdRatioBased(configProperties.getDouble("otel.traces.sampler.arg", 1.0)));
            default -> {
                for (ConfigurableSamplerProvider provider : ServiceLoader.load(ConfigurableSamplerProvider.class, tccl)) {
                    if (provider.getName().equals(samplerName)) {
                        yield provider.createSampler(configProperties);
                    }
                }
                yield Sampler.parentBased(Sampler.alwaysOn());
            }
        };
    }

    private ContextPropagators buildPropagators(SdkCustomizers customizers, ClassLoader tccl) {
        String propagatorNames = configProperties.getString("otel.propagators", "tracecontext,baggage");
        List<TextMapPropagator> propagators = new ArrayList<>();
        for (String name : propagatorNames.split(",")) {
            TextMapPropagator propagator = loadPropagator(name.trim(), tccl);
            if (propagator != null) {
                for (BiFunction<? super TextMapPropagator, ConfigProperties, ? extends TextMapPropagator> fn : customizers
                        .getPropagatorCustomizers()) {
                    propagator = fn.apply(propagator, configProperties);
                }
                propagators.add(propagator);
            }
        }
        return ContextPropagators.create(TextMapPropagator.composite(propagators));
    }

    private TextMapPropagator loadPropagator(String name, ClassLoader tccl) {
        return switch (name) {
            case "tracecontext" -> W3CTraceContextPropagator.getInstance();
            case "baggage" -> W3CBaggagePropagator.getInstance();
            default -> {
                for (ConfigurablePropagatorProvider provider : ServiceLoader.load(ConfigurablePropagatorProvider.class,
                        tccl)) {
                    if (provider.getName().equals(name)) {
                        yield provider.getPropagator(configProperties);
                    }
                }
                yield null;
            }
        };
    }

    private SdkMeterProvider buildMeterProvider(Resource resource, SdkCustomizers customizers) {
        SdkMeterProviderBuilder builder = SdkMeterProvider.builder().setResource(resource);

        String exporterName = configProperties.getString("otel.metrics.exporter", "otlp");
        if (!"none".equals(exporterName)) {
            MetricExporter exporter = loadMetricExporter(exporterName);
            if (exporter != null) {
                for (BiFunction<? super MetricExporter, ConfigProperties, ? extends MetricExporter> fn : customizers
                        .getMetricExporterCustomizers()) {
                    exporter = fn.apply(exporter, configProperties);
                }
                long intervalMillis = configProperties.getLong("otel.metric.export.interval", 60000);
                MetricReader reader = PeriodicMetricReader.builder(exporter)
                        //                        .setExecutor(Executors.newSingleThreadScheduledExecutor())
                        .setInterval(Duration.ofMillis(intervalMillis))
                        .build();
                for (BiFunction<? super MetricReader, ConfigProperties, ? extends MetricReader> fn : customizers
                        .getMetricReaderCustomizers()) {
                    reader = fn.apply(reader, configProperties);
                }
                builder.registerMetricReader(reader);
            }
        }

        for (BiFunction<SdkMeterProviderBuilder, ConfigProperties, SdkMeterProviderBuilder> fn : customizers
                .getMeterProviderCustomizers()) {
            builder = fn.apply(builder, configProperties);
        }

        return builder.build();
    }

    private SdkLoggerProvider buildLoggerProvider(Resource resource, SdkCustomizers customizers) {
        SdkLoggerProviderBuilder builder = SdkLoggerProvider.builder()
                .setResource(resource);

        String exporterName = configProperties.getString("otel.logs.exporter", "otlp");
        if (!"none".equals(exporterName)) {
            LogRecordExporter exporter = loadLogRecordExporter(exporterName);
            if (exporter != null) {
                for (BiFunction<? super LogRecordExporter, ConfigProperties, ? extends LogRecordExporter> fn : customizers
                        .getLogRecordExporterCustomizers()) {
                    exporter = fn.apply(exporter, configProperties);
                }
                LogRecordProcessor processor = BatchLogRecordProcessor.builder(exporter).build();
                for (BiFunction<? super LogRecordProcessor, ConfigProperties, ? extends LogRecordProcessor> fn : customizers
                        .getLogRecordProcessorCustomizers()) {
                    processor = fn.apply(processor, configProperties);
                }
                builder.addLogRecordProcessor(processor);
            }
        }

        for (BiFunction<SdkLoggerProviderBuilder, ConfigProperties, SdkLoggerProviderBuilder> fn : customizers
                .getLoggerProviderCustomizers()) {
            builder = fn.apply(builder, configProperties);
        }

        return builder.build();
    }

    private SpanExporter loadSpanExporter(String name, ClassLoader tccl) {
        for (ConfigurableSpanExporterProvider provider : ServiceLoader.load(ConfigurableSpanExporterProvider.class,
                tccl)) {
            if (provider.getName().equals(name)) {
                return provider.createExporter(configProperties);
            }
        }
        return null;
    }

    private MetricExporter loadMetricExporter(String name) {
        for (ConfigurableMetricExporterProvider provider : ServiceLoader.load(ConfigurableMetricExporterProvider.class,
                Thread.currentThread().getContextClassLoader())) {
            if (provider.getName().equals(name)) {
                return provider.createExporter(configProperties);
            }
        }
        return null;
    }

    private LogRecordExporter loadLogRecordExporter(String name) {
        for (ConfigurableLogRecordExporterProvider provider : ServiceLoader.load(
                ConfigurableLogRecordExporterProvider.class,
                Thread.currentThread().getContextClassLoader())) {
            if (provider.getName().equals(name)) {
                return provider.createExporter(configProperties);
            }
        }
        return null;
    }
}
