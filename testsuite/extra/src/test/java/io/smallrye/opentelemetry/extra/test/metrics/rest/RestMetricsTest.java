package io.smallrye.opentelemetry.extra.test.metrics.rest;

import static io.opentelemetry.sdk.metrics.data.MetricDataType.HISTOGRAM;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.smallrye.opentelemetry.test.InMemoryExporter;

@ExtendWith(ArquillianExtension.class)
public class RestMetricsTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class);
    }

    @ArquillianResource
    URL url;
    @Inject
    InMemoryExporter exporter;

    @BeforeEach
    void setUp() {
        // The Metrics statistics will not reset (counters, histograms, etc.)
        exporter.reset();
    }

    @Test
    void metricAttributes() {
        given().get("/span").then().statusCode(HTTP_OK);
        given().get("/span/1").then().statusCode(HTTP_OK);
        given().get("/span/2").then().statusCode(HTTP_OK);
        given().get("/span/2").then().statusCode(HTTP_OK);

        MetricData metricsSpan = exporter.getLastFinishedHistogramItem("http.server.request.duration", url.getPath() + "span",
                1);
        assertEquals("Duration of HTTP server requests.", metricsSpan.getDescription());
        assertEquals(HISTOGRAM, metricsSpan.getType());
        assertEquals(1, metricsSpan.getData().getPoints().size());
        assertTrue(metricsSpan.getData().getPoints().iterator().next() instanceof HistogramPointData);
        HistogramPointData pointSpan = (HistogramPointData) metricsSpan.getData().getPoints().iterator().next();
        assertNotNull(pointSpan.getAttributes());
        assertFalse(pointSpan.getAttributes().isEmpty());
        assertEquals(HTTP_OK, pointSpan.getAttributes().get(HTTP_RESPONSE_STATUS_CODE));
        assertEquals(HttpMethod.GET, pointSpan.getAttributes().get(HTTP_REQUEST_METHOD));
        assertEquals(url.getPath() + "span", pointSpan.getAttributes().get(HTTP_ROUTE));
        assertTrue(pointSpan.getStartEpochNanos() > 0);
        assertTrue(pointSpan.getEpochNanos() > 0);
        assertEquals(1, pointSpan.getCount());

        MetricData metricsSpanName = exporter.getLastFinishedHistogramItem("http.server.request.duration",
                url.getPath() + "span/{name}", 3);
        assertEquals("Duration of HTTP server requests.", metricsSpanName.getDescription());
        assertEquals(HISTOGRAM, metricsSpanName.getType());
        assertEquals(1, metricsSpanName.getData().getPoints().size());
        assertTrue(metricsSpanName.getData().getPoints().iterator().next() instanceof HistogramPointData);
        HistogramPointData pointSpanName = (HistogramPointData) metricsSpanName.getData().getPoints().iterator().next();
        assertNotNull(pointSpanName.getAttributes());
        assertFalse(pointSpanName.getAttributes().isEmpty());
        assertEquals(HTTP_OK, pointSpanName.getAttributes().get(HTTP_RESPONSE_STATUS_CODE));
        assertEquals(HttpMethod.GET, pointSpanName.getAttributes().get(HTTP_REQUEST_METHOD));
        assertEquals(url.getPath() + "span/{name}", pointSpanName.getAttributes().get(HTTP_ROUTE));
        assertTrue(pointSpanName.getStartEpochNanos() > 0);
        assertTrue(pointSpanName.getEpochNanos() > 0);
        assertEquals(3, pointSpanName.getCount());
    }

    @Test
    void metrics() {
        given().get("/sub/12").then().statusCode(HTTP_OK);
        assertNotNull(exporter.getFinishedMetricItem("queueSize"));
        assertNotNull(exporter.getFinishedMetricItem("http.server.request.duration"));
        assertNotNull(exporter.getFinishedMetricItem("http.server.active_requests"));
        assertNotNull(exporter.getFinishedMetricItem("processedSpans"));
    }

    @Path("/")
    public static class SpanResource {
        @GET
        @Path("/span")
        public Response span() {
            return Response.ok().build();
        }

        @GET
        @Path("/span/{name}")
        public Response spanName(@PathParam(value = "name") String name) {
            return Response.ok().build();
        }

        @POST
        @Path("/span")
        public Response spanPost(String payload) {
            return Response.ok(payload).build();
        }

        @Path("/sub/{id}")
        public SubResource subResource(@PathParam("id") String id) {
            return new SubResource(id);
        }
    }

    public static class SubResource {
        private final String id;

        public SubResource(final String id) {
            this.id = id;
        }

        @GET
        public Response get() {
            return Response.ok().build();
        }
    }

    @ApplicationPath("/")
    public static class RestApplication extends Application {

    }
}
