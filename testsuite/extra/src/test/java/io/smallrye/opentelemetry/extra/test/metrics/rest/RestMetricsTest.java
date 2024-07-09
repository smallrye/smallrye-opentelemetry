package io.smallrye.opentelemetry.extra.test.metrics.rest;

import static io.opentelemetry.sdk.metrics.data.MetricDataType.HISTOGRAM;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;

import java.net.URL;
import java.util.List;
import java.util.Map;

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

        metricExporter.assertCountAtLeast("http.server.request.duration", url.getPath() + "span", 1);
        metricExporter.assertCountAtLeast("http.server.request.duration", url.getPath() + "span/{name}", 3);
        List<MetricData> finishedMetricItems = metricExporter.getFinishedMetricItems("http.server.request.duration", null);
        System.out.println(finishedMetricItems.size());

        assertThat(finishedMetricItems, allOf(
                everyItem(hasProperty("name", equalTo("http.server.request.duration"))),
                everyItem(hasProperty("type", equalTo(HISTOGRAM)))));

        /*
         * TODO - Fix this - Flaky test
         * Map<String, PointData> pointDataMap = getMostRecentPointsMap(finishedMetricItems);
         * assertEquals(1, getCount(pointDataMap,
         * "http.request.method:GET,http.response.status_code:200,http.route:" + url.getPath() + "span"));
         * assertEquals(3, getCount(pointDataMap,
         * "http.request.method:GET,http.response.status_code:200,http.route:" + url.getPath() + "span/{name}"));
         */
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
        metricExporter.assertCountAtLeast("http.server.request.duration", url.getPath() + "span/{name}", 1);
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
