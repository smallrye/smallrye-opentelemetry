package io.smallrye.opentelemetry.tck.rest;

import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.List;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.smallrye.opentelemetry.tck.InMemorySpanExporter;

@ExtendWith(ArquillianExtension.class)
class RestSpanTest {
    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class);
    }

    InMemorySpanExporter spanExporter;

    @ArquillianResource
    private URL url;

    @BeforeEach
    void setUp() {
        spanExporter = InMemorySpanExporter.HOLDER.get();
        spanExporter.reset();
    }

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void span() {
        given().get("/span").then().statusCode(HTTP_OK);

        List<SpanData> spanItems = spanExporter.getFinishedSpanItems();
        assertEquals(1, spanItems.size());
        assertEquals(SERVER, spanItems.get(0).getKind());
        assertEquals(url.getPath() + "span", spanItems.get(0).getName());
        assertEquals(HTTP_OK, spanItems.get(0).getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, spanItems.get(0).getAttributes().get(HTTP_METHOD));
    }

    @Test
    void spanName() {
        given().get("/span/1").then().statusCode(HTTP_OK);

        List<SpanData> spanItems = spanExporter.getFinishedSpanItems();
        assertEquals(1, spanItems.size());
        assertEquals(SERVER, spanItems.get(0).getKind());
        assertEquals(url.getPath() + "span/{name}", spanItems.get(0).getName());
        assertEquals(HTTP_OK, spanItems.get(0).getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, spanItems.get(0).getAttributes().get(HTTP_METHOD));
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
