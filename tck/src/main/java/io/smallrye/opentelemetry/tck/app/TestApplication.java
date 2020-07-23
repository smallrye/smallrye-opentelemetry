package io.smallrye.opentelemetry.tck.app;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import io.opentelemetry.exporters.otlp.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.smallrye.opentelemetry.tck.BaseTests;

/**
 * @author Pavol Loffay
 */
@ApplicationPath(TestApplication.PATH_ROOT)
public class TestApplication extends Application {

    public static final String PATH_ROOT = "test_app";

    // This code has to run only once, otherwise it will install multiple exporters
    // which results in data being reported multiple times
    static {
        TracerSdkProvider tracerProvider = OpenTelemetrySdk.getTracerProvider();
        // add OTLP exporter
        OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.newBuilder()
                .setEndpoint(String.format("localhost:%d", BaseTests.OTLP_SERVICE_PORT))
                .build();
        tracerProvider.addSpanProcessor(SimpleSpanProcessor.newBuilder(exporter).build());
    }
}
