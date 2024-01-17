package io.smallrye.opentelemetry.implementation.observation;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.PRODUCER;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.smallrye.common.constraint.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.transport.ReceiverContext;
import io.micrometer.observation.transport.SenderContext;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.smallrye.config.inject.ConfigExtension;
import io.smallrye.opentelemetry.implementation.cdi.OpenTelemetryExtension;
import io.smallrye.opentelemetry.implementation.config.OpenTelemetryConfigProducer;
import io.smallrye.opentelemetry.instrumentation.observation.ObservationRegistryProducer;
import io.smallrye.opentelemetry.instrumentation.observation.handler.OpenTelemetryObservationHandler;
import io.smallrye.opentelemetry.test.InMemoryMetricExporter;
import io.smallrye.opentelemetry.test.InMemoryMetricExporterProvider;
import io.smallrye.opentelemetry.test.InMemorySpanExporter;
import io.smallrye.opentelemetry.test.InMemorySpanExporterProvider;

@EnableAutoWeld
@AddExtensions({ OpenTelemetryExtension.class, ConfigExtension.class })
@AddBeanClasses({ OpenTelemetryConfigProducer.class, ObservationRegistryProducer.class, OpenTelemetryObservationHandler.class,
        InMemoryMetricExporter.class, InMemoryMetricExporterProvider.class,
        InMemorySpanExporter.class, InMemorySpanExporterProvider.class, })
class ObservationOTelTest {
    @Inject
    InMemorySpanExporter spanExporter;
    @Inject
    SpanBean spanBean;
    @Inject
    ObservationSpan observationSpan;
    @Inject
    ObservationRegistry observationRegistry;

    @BeforeEach
    void setUp() {
        spanExporter.reset();
        GlobalOpenTelemetry.resetForTest();
    }

    @Test
    void observationSpan() {
        observationSpan.createObservationSpan();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals("createObservationSpan", spanItems.get(0).getName());
        assertEquals("lowTagValue", spanItems.get(0).getAttributes().asMap().get(AttributeKey.stringKey("lowTag")));
    }

    @Test
    void OtelParentAndObservationChildSpan() {
        spanBean.observationSpanChild();
        List<SpanData> spanData = getSpanDataAndExpectCount(2);
        SpanData parentSpan = spanData.get(0);
        assertEquals("SpanBean.observationSpanChild", parentSpan.getName());

        SpanData childSpan = spanData.get(1);
        assertEquals("createObservationSpan", childSpan.getName());
        assertEquals("lowTagValue", childSpan.getAttributes().asMap().get(AttributeKey.stringKey("lowTag")));
        assertEquals(parentSpan.getSpanId(), childSpan.getParentSpanId());
    }

    @Test
    void ObservationParentAndOTelChildSpan() {
        observationSpan.spanChild();
        List<SpanData> spanData = getSpanDataAndExpectCount(2);
        SpanData parentSpan = spanData.get(0);
        assertEquals("createObservationSpanWithChild", parentSpan.getName());
        assertEquals("lowTagValue", parentSpan.getAttributes().asMap().get(AttributeKey.stringKey("lowTag")));

        SpanData childSpan = spanData.get(1);
        assertEquals("SpanChildBean.spanChild", childSpan.getName());
        assertEquals(parentSpan.getSpanId(), childSpan.getParentSpanId());
    }

    @Test
    void otelSpan() {
        spanBean.span();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals("SpanBean.span", spanItems.get(0).getName());
        assertEquals(INTERNAL, spanItems.get(0).getKind());
    }

    @Test
    void otelSpanChild() {
        spanBean.spanChild();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(2);
        assertEquals("SpanChildBean.spanChild", spanItems.get(0).getName());
        assertEquals("SpanBean.spanChild", spanItems.get(1).getName());
        assertEquals(spanItems.get(0).getParentSpanId(), spanItems.get(1).getSpanId());
    }

    @Test
    void testSenderContextPropagation() {
        Map<String, String> outboundCarrier = new HashMap<>();

        SenderContext senderContext = new SenderContext<Map<String, String>>(
                (carrier1, key, value) -> carrier1.put(key, value));
        senderContext.setCarrier(outboundCarrier);

        Observation observation = Observation.start("test", () -> senderContext, observationRegistry);
        observation.stop();

        assertNotNull(outboundCarrier.get("traceparent"));
        List<SpanData> finishedSpanItems = getSpanDataAndExpectCount(1);
        assertEquals(PRODUCER, finishedSpanItems.get(0).getKind());
        assertTrue(outboundCarrier.get("traceparent").contains(finishedSpanItems.get(0).getTraceId()),
                "traceId not found in traceparent");
    }

    @Test
    void testReceiverContextPropagation() {
        String traceId = "0af7651916cd43dd8448eb211c80319c";
        Map<String, String> inboundCarrier = Map.of("traceparent", "00-" + traceId + "-b7ad6b7169203331-01");

        ReceiverContext receiverContext = new ReceiverContext<Map<String, String>>((carrier, key) -> carrier.get(key));
        receiverContext.setCarrier(inboundCarrier);

        Observation observation = Observation.start("test", () -> receiverContext, observationRegistry);
        observation.stop();

        List<SpanData> finishedSpanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals(traceId, finishedSpanItems.get(0).getTraceId());
    }

    private List<SpanData> getSpanDataAndExpectCount(Integer spanCount) {
        List<SpanData> finishedSpanItems = spanExporter.getFinishedSpanItems(spanCount);
        return finishedSpanItems.stream()
                .sorted((o1, o2) -> (int) (o1.getStartEpochNanos() - o2.getStartEpochNanos()))
                .collect(Collectors.toList());
    }

    @ApplicationScoped
    public static class SpanBean {
        @Inject
        SpanChildBean spanChildBean;
        @Inject
        ObservationSpan observationSpan;

        @WithSpan
        public void span() {

        }

        @WithSpan("name")
        public void spanName() {

        }

        @WithSpan(kind = SERVER)
        public void spanKind() {

        }

        @WithSpan
        public void spanArgs(@SpanAttribute(value = "arg") String arg) {

        }

        @WithSpan
        public void spanChild() {
            spanChildBean.spanChild();
        }

        @WithSpan
        public void observationSpanChild() {
            observationSpan.createObservationSpan();
        }
    }

    @ApplicationScoped
    public static class SpanChildBean {
        @WithSpan
        public void spanChild() {

        }
    }

    @ApplicationScoped
    public static class ObservationSpan {
        @Inject
        ObservationRegistry observationRegistry;
        @Inject
        SpanChildBean spanChildBean;

        public void createObservationSpan() {
            Observation observation = Observation.start("createObservationSpan", observationRegistry);
            observation.lowCardinalityKeyValue("lowTag", "lowTagValue");
            observation.stop();
        }

        public void spanChild() {
            Observation obs = Observation.createNotStarted("createObservationSpanWithChild", observationRegistry);
            obs.observe(() -> {
                obs.lowCardinalityKeyValue("lowTag", "lowTagValue");
                spanChildBean.spanChild();
            });
        }
    }
}
