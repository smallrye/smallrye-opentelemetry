package io.smallrye.opentelemetry.tck.rest;

import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_VERSION;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_TARGET;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.smallrye.opentelemetry.api.OpenTelemetryConfig;
import io.smallrye.opentelemetry.tck.InMemorySpanExporter;

@ExtendWith(ArquillianExtension.class)
class RestSpanTest {
    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class);
    }

    @ArquillianResource
    URL url;
    @Inject
    InMemorySpanExporter spanExporter;

    @BeforeEach
    void setUp() {
        spanExporter.reset();
    }

    @Test
    void span() {
        given().get("/span").then().statusCode(HTTP_OK);

        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals(1, spanItems.size());
        assertEquals(SERVER, spanItems.get(0).getKind());
        assertEquals(url.getPath() + "span", spanItems.get(0).getName());
        assertEquals(HTTP_OK, spanItems.get(0).getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, spanItems.get(0).getAttributes().get(HTTP_METHOD));

        assertEquals("tck", spanItems.get(0).getResource().getAttribute(SERVICE_NAME));
        assertEquals("0.1.0-SNAPSHOT", spanItems.get(0).getResource().getAttribute(SERVICE_VERSION));

        InstrumentationScopeInfo libraryInfo = spanItems.get(0).getInstrumentationScopeInfo();
        assertEquals(OpenTelemetryConfig.INSTRUMENTATION_NAME, libraryInfo.getName());
        assertEquals(OpenTelemetryConfig.INSTRUMENTATION_VERSION, libraryInfo.getVersion());
    }

    @Test
    void spanName() {
        given().get("/span/1").then().statusCode(HTTP_OK);

        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals(1, spanItems.size());
        assertEquals(SERVER, spanItems.get(0).getKind());
        assertEquals(url.getPath() + "span/{name}", spanItems.get(0).getName());
        assertEquals(HTTP_OK, spanItems.get(0).getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, spanItems.get(0).getAttributes().get(HTTP_METHOD));
    }

    @Test
    void spanNameWithoutQueryString() {
        given().get("/span/1?id=1").then().statusCode(HTTP_OK);

        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals(1, spanItems.size());
        assertEquals(SERVER, spanItems.get(0).getKind());
        assertEquals(url.getPath() + "span/{name}", spanItems.get(0).getName());
        assertEquals(HTTP_OK, spanItems.get(0).getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, spanItems.get(0).getAttributes().get(HTTP_METHOD));
        assertEquals(url.getPath() + "span/1?id=1", spanItems.get(0).getAttributes().get(HTTP_TARGET));
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
    }

    @ApplicationPath("/")
    public static class RestApplication extends Application {

    }
}
