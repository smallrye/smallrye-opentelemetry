package io.smallrye.opentelemetry.implementation.cdi;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.smallrye.config.inject.ConfigExtension;
import io.smallrye.opentelemetry.implementation.config.OpenTelemetryConfigProducer;

@EnableAutoWeld
@AddExtensions({ OpenTelemetryExtension.class, ConfigExtension.class })
@AddBeanClasses({ OpenTelemetryConfigProducer.class, InMemoryMetricExporterProvider.class, InMemoryMetricExporter.class })
class WithSpanInterceptorTest {
    @Inject
    InMemorySpanExporter spanExporter;
    @Inject
    SpanBean spanBean;

    @BeforeEach
    void setUp() {
        spanExporter.reset();
        GlobalOpenTelemetry.resetForTest();
    }

    @Test
    void span() {
        spanBean.span();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals("SpanBean.span", spanItems.get(0).getName());
        assertEquals(INTERNAL, spanItems.get(0).getKind());
    }

    @Test
    void spanName() {
        spanBean.spanName();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals("name", spanItems.get(0).getName());
        assertEquals(INTERNAL, spanItems.get(0).getKind());
    }

    @Test
    void spanKind() {
        spanBean.spanKind();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals("SpanBean.spanKind", spanItems.get(0).getName());
        assertEquals(SERVER, spanItems.get(0).getKind());
    }

    @Test
    void spanArgs() {
        spanBean.spanArgs("argument");
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals("SpanBean.spanArgs", spanItems.get(0).getName());
        assertEquals(INTERNAL, spanItems.get(0).getKind());
        assertEquals("argument", spanItems.get(0).getAttributes().get(AttributeKey.stringKey("arg")));
    }

    @Test
    void spanChild() {
        spanBean.spanChild();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(2);
        assertEquals("SpanChildBean.spanChild", spanItems.get(0).getName());
        assertEquals("SpanBean.spanChild", spanItems.get(1).getName());
        assertEquals(spanItems.get(0).getParentSpanId(), spanItems.get(1).getSpanId());
    }

    @ApplicationScoped
    public static class SpanBean {
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

        @Inject
        SpanChildBean spanChildBean;

        @WithSpan
        public void spanChild() {
            spanChildBean.spanChild();
        }
    }

    @ApplicationScoped
    public static class SpanChildBean {
        @WithSpan
        public void spanChild() {

        }
    }
}
