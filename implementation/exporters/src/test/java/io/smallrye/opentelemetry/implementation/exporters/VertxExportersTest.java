package io.smallrye.opentelemetry.implementation.exporters;

import static io.smallrye.opentelemetry.implementation.exporters.Constants.MIMETYPE_PROTOBUF;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_ENDPOINT;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_TRACES_PROTOCOL;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.PROTOCOL_GRPC;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.internal.grpc.GrpcExporter;
import io.opentelemetry.exporter.internal.http.HttpExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.common.InternalTelemetryVersion;
import io.opentelemetry.sdk.common.internal.ComponentId;
import io.opentelemetry.sdk.common.internal.StandardComponentId;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.smallrye.opentelemetry.implementation.exporters.sender.VertxGrpcSender;
import io.smallrye.opentelemetry.implementation.exporters.sender.VertxHttpSender;
import io.smallrye.opentelemetry.implementation.exporters.traces.VertxGrpcSpanExporter;
import io.smallrye.opentelemetry.implementation.exporters.traces.VertxHttpSpanExporter;

/**
 * This test exercises the Vertx-based exporters directly, bypassing OTel autoconfiguration.
 * It creates exporters using {@link OtelConfigPropertiesConfiguration} and sends trace data
 * to a testcontainers-based OpenTelemetry Collector.
 */
public class VertxExportersTest {
    private static OpenTelemetryCollectorContainer otelCollector;

    @BeforeAll
    public static void setup() {
        Assumptions.assumeTrue(isDockerAvailable(), "Docker is not available.");

        if (otelCollector == null) {
            otelCollector = new OpenTelemetryCollectorContainer();
            otelCollector.start();
        }
    }

    @AfterAll
    public static void tearDown() {
        if (otelCollector != null) {
            otelCollector.stop();
            otelCollector = null;
        }
    }

    @Test
    public void testGrpcExporter() {
        testExporterByProtocol("grpc");
    }

    @Test
    public void testHttpExporter() {
        testExporterByProtocol("http/protobuf");
    }

    private void testExporterByProtocol(String protocol) {
        String endpoint = PROTOCOL_GRPC.equals(protocol) ? otelCollector.getOtlpGrpcEndpoint()
                : otelCollector.getOtlpHttpEndpoint();

        ConfigProperties configProps = DefaultConfigProperties.createFromMap(Map.of(
                OTEL_EXPORTER_OTLP_TRACES_PROTOCOL, protocol,
                OTEL_EXPORTER_OTLP_ENDPOINT, endpoint));

        ExporterConfiguration configuration = new OtelConfigPropertiesConfiguration(configProps, "span");

        SpanExporter spanExporter;
        URI baseUri = configuration.getEndpoint();

        if (PROTOCOL_GRPC.equals(protocol)) {
            GrpcExporter grpcExporter = new GrpcExporter(
                    new VertxGrpcSender(VertxGrpcSender.GRPC_TRACE_SERVICE_NAME, configuration),
                    InternalTelemetryVersion.LATEST,
                    ComponentId.generateLazy(StandardComponentId.ExporterType.OTLP_GRPC_SPAN_EXPORTER),
                    MeterProvider::noop,
                    baseUri);
            spanExporter = new VertxGrpcSpanExporter(grpcExporter);
        } else {
            HttpExporter httpExporter = new HttpExporter(
                    ComponentId.generateLazy(StandardComponentId.ExporterType.OTLP_HTTP_SPAN_EXPORTER),
                    new VertxHttpSender(VertxHttpSender.TRACES_PATH, MIMETYPE_PROTOBUF, configuration),
                    MeterProvider::noop,
                    InternalTelemetryVersion.LATEST,
                    baseUri,
                    false);
            spanExporter = new VertxHttpSpanExporter(httpExporter);
        }

        final String tracerName = "smallrye.opentelemetry.test." + protocol;
        final String spanName = protocol + " test trace";
        final String eventName = protocol + " test event";

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter)
                        .setScheduleDelay(Duration.ofMillis(1))
                        .build())
                .build();
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();

        try {
            Tracer tracer = openTelemetry.getTracer(tracerName);
            Span span = tracer.spanBuilder(spanName).startSpan();
            span.addEvent(eventName);
            span.end();

            boolean found = false;
            int count = 0;
            while (!found && count < 10) {
                String logs = otelCollector.getLogs();
                found = logs.contains(tracerName) &&
                        logs.contains(spanName) &&
                        logs.contains(eventName);
                if (!found) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    count++;
                }
            }

            Assertions.assertTrue(found, "Trace data not found.");
        } finally {
            tracerProvider.shutdown();
        }
    }

    private static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
