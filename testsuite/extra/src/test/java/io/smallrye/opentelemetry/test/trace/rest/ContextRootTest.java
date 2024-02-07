package io.smallrye.opentelemetry.test.trace.rest;

import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_ROUTE;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
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

import io.opentelemetry.sdk.trace.data.SpanData;
import io.smallrye.opentelemetry.test.trace.InMemorySpanExporter;

@ExtendWith(ArquillianExtension.class)
class ContextRootTest {
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
    void route() {
        given().get("/application/resource/span").then().statusCode(HTTP_OK);

        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals(1, spanItems.size());
        assertEquals(SERVER, spanItems.get(0).getKind());
        assertEquals(url.getPath() + "application/resource/span", spanItems.get(0).getAttributes().get(HTTP_ROUTE));
    }

    @ApplicationPath("/application")
    public static class RestApplication extends Application {

    }

    @Path("/resource")
    public static class Resource {
        @GET
        @Path("/span")
        public Response span() {
            return Response.ok().build();
        }
    }
}
