package io.smallrye.opentelemetry.tck.rest;

import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static java.net.HttpURLConnection.HTTP_OK;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
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

import io.opentelemetry.sdk.trace.data.SpanData;
import io.smallrye.opentelemetry.tck.TestSpanExporter;

@ExtendWith(ArquillianExtension.class)
public class RestSpanTest {
    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap
                .create(WebArchive.class)
                .addClass(RestApplication.class)
                .addClass(RestSpan.class);
    }

    @ArquillianResource
    URL url;
    @Inject
    TestSpanExporter spanExporter;

    @BeforeEach
    void setUp() {
        spanExporter.reset();
    }

    @Test
    void span() {
        WebTarget echoEndpointTarget = ClientBuilder.newClient().target(url.toExternalForm() + "/span");
        Response response = echoEndpointTarget.request(TEXT_PLAIN).get();
        assertEquals(response.getStatus(), HTTP_OK);

        List<SpanData> spanItems = spanExporter.getFinishedSpanItems();
        assertEquals(1, spanItems.size());
        assertEquals(SERVER, spanItems.get(0).getKind());
        assertEquals("/span", spanItems.get(0).getName());
        assertEquals(HTTP_OK, spanItems.get(0).getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, spanItems.get(0).getAttributes().get(HTTP_METHOD));
    }

    @Test
    void spanName() {
        WebTarget echoEndpointTarget = ClientBuilder.newClient().target(url.toExternalForm() + "/span/1");
        Response response = echoEndpointTarget.request(TEXT_PLAIN).get();
        assertEquals(response.getStatus(), HTTP_OK);

        List<SpanData> spanItems = spanExporter.getFinishedSpanItems();
        assertEquals(1, spanItems.size());
        assertEquals(SERVER, spanItems.get(0).getKind());
        assertEquals("/span/{name}", spanItems.get(0).getName());
        assertEquals(HTTP_OK, spanItems.get(0).getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, spanItems.get(0).getAttributes().get(HTTP_METHOD));
    }

    @Path("/")
    public static class RestSpan {
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
