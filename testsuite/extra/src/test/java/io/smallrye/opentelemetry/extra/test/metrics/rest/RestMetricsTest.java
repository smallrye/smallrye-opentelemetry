package io.smallrye.opentelemetry.extra.test.metrics.rest;

import static io.opentelemetry.sdk.metrics.data.MetricDataType.HISTOGRAM;
import static io.restassured.RestAssured.given;
import static io.smallrye.opentelemetry.test.InMemoryMetricExporter.getMostRecentPointsMap;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.opentelemetry.instrumentation.api.internal.SemconvStability;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.smallrye.opentelemetry.test.InMemoryMetricExporter;

@ExtendWith(ArquillianExtension.class)
public class RestMetricsTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class);
    }

    @ArquillianResource
    URL url;
    @Inject
    InMemoryMetricExporter metricExporter;

    @AfterEach
    void reset() {
        // important, metrics continue to arrive after reset.
        metricExporter.reset();
    }

    @Test
    void metricAttributes() {
        given().get("/span").then().statusCode(HTTP_OK);
        given().get("/span/1").then().statusCode(HTTP_OK);
        given().get("/span/2").then().statusCode(HTTP_OK);
        given().get("/span/2").then().statusCode(HTTP_OK);

        metricExporter.assertCountAtLeast("http.server.duration", "/span", 1);
        metricExporter.assertCountAtLeast("http.server.duration", "/span/1", 1);
        metricExporter.assertCountAtLeast("http.server.duration", "/span/2", 2);
        List<MetricData> finishedMetricItems = metricExporter.getFinishedMetricItems("http.server.duration", null);

        assertThat(finishedMetricItems, allOf(
                everyItem(hasProperty("name", equalTo("http.server.duration"))),
                everyItem(hasProperty("type", equalTo(HISTOGRAM)))));

        Map<String, PointData> pointDataMap = getMostRecentPointsMap(finishedMetricItems);
        if (SemconvStability.emitOldHttpSemconv()) {
            assertEquals(1, getCount(pointDataMap, "http.method:GET,http.route:/span,http.status_code:200"),
                    finishedMetricItems.toString());
            assertEquals(1, getCount(pointDataMap, "http.method:GET,http.route:/span/1,http.status_code:200"),
                    finishedMetricItems.toString());
            assertEquals(2, getCount(pointDataMap, "http.method:GET,http.route:/span/2,http.status_code:200"),
                    finishedMetricItems.toString());
        } else {
            assertEquals(1, getCount(pointDataMap, "http.request.method:GET,http.response.status_code:200,http.route:/span"),
                    pointDataMap.keySet().stream()
                            .collect(Collectors.joining("**")));
            assertEquals(1, getCount(pointDataMap, "http.request.method:GET,http.response.status_code:200,http.route:/span/1"),
                    pointDataMap.keySet().stream()
                            .collect(Collectors.joining("**")));
            assertEquals(2, getCount(pointDataMap, "http.request.method:GET,http.response.status_code:200,http.route:/span/2"),
                    pointDataMap.keySet().stream()
                            .collect(Collectors.joining("**")));
        }
    }

    private long getCount(final Map<String, PointData> pointDataMap, final String key) {
        HistogramPointData histogramPointData = (HistogramPointData) pointDataMap.get(key);
        if (histogramPointData == null) {
            return 0;
        }
        return histogramPointData.getCount();
    }

    @Test
    void metrics() {
        given().get("/span/12").then().statusCode(HTTP_OK);
        metricExporter.assertCountAtLeast("queueSize", null, 1);
        metricExporter.assertCountAtLeast("http.server.duration", "/span/12", 1);
        metricExporter.assertCountAtLeast("http.server.active_requests", null, 1);
        metricExporter.assertCountAtLeast("processedSpans", null, 1);
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
