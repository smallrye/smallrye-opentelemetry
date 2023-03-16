package io.smallrye.opentelemetry.test.rest;

import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_VERSION;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_CLIENT_IP;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_FLAVOR;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_SCHEME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_TARGET;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_USER_AGENT;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_NAME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_PORT;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_SOCK_FAMILY;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_SOCK_HOST_ADDR;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_SOCK_HOST_PORT;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_SOCK_PEER_ADDR;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_SOCK_PEER_NAME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_SOCK_PEER_PORT;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URL;
import java.util.List;

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

import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.smallrye.opentelemetry.api.OpenTelemetryConfig;
import io.smallrye.opentelemetry.test.InMemorySpanExporter;

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
        assertEquals(HttpMethod.GET + " " + url.getPath() + "span", spanItems.get(0).getName());
        assertEquals(HTTP_OK, spanItems.get(0).getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, spanItems.get(0).getAttributes().get(HTTP_METHOD));

        assertEquals("tck", spanItems.get(0).getResource().getAttribute(SERVICE_NAME));
        assertEquals("1.0", spanItems.get(0).getResource().getAttribute(SERVICE_VERSION));

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
        assertEquals(HttpMethod.GET + " " + url.getPath() + "span/{name}", spanItems.get(0).getName());
        assertEquals(HTTP_OK, spanItems.get(0).getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, spanItems.get(0).getAttributes().get(HTTP_METHOD));
    }

    @Test
    void spanNameWithoutQueryString() {
        given().get("/span/1?id=1").then().statusCode(HTTP_OK);

        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals(1, spanItems.size());
        assertEquals(SERVER, spanItems.get(0).getKind());
        assertEquals(HttpMethod.GET + " " + url.getPath() + "span/{name}", spanItems.get(0).getName());
        assertEquals(HTTP_OK, spanItems.get(0).getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, spanItems.get(0).getAttributes().get(HTTP_METHOD));
        assertEquals(url.getPath() + "span/1?id=1", spanItems.get(0).getAttributes().get(HTTP_TARGET));
        assertEquals(url.getPath() + "span/{name}", spanItems.get(0).getAttributes().get(HTTP_ROUTE));
    }

    @Test
    void spanPost() {
        given().body("payload").post("/span").then().statusCode(HTTP_OK);

        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals(1, spanItems.size());
        SpanData span = spanItems.get(0);
        assertEquals(SERVER, span.getKind());
        assertEquals(HttpMethod.POST + " " + url.getPath() + "span", span.getName());

        // Common Attributes
        assertEquals(HttpMethod.POST, span.getAttributes().get(HTTP_METHOD)); // http.method
        assertEquals(HTTP_OK, span.getAttributes().get(HTTP_STATUS_CODE)); // http.status_code
        assertNotNull(span.getAttributes().get(HTTP_FLAVOR)); // http.flavor
        assertNotNull(span.getAttributes().get(HTTP_USER_AGENT)); // http.user_agent
        assertNotNull(span.getAttributes().get(HTTP_REQUEST_CONTENT_LENGTH)); // http.request_content_length
        // assertNotNull(spanItems.get(0).getAttributes().get(HTTP_RESPONSE_CONTENT_LENGTH));       // http.response_content_length
        assertNull(span.getAttributes().get(NET_SOCK_FAMILY)); // net.sock.family
        assertNull(span.getAttributes().get(NET_SOCK_PEER_ADDR)); // net.sock.peer.addr
        assertNull(span.getAttributes().get(NET_SOCK_PEER_NAME)); // net.sock.peer.name
        assertNull(span.getAttributes().get(NET_SOCK_PEER_PORT)); // net.sock.peer.port

        // Server Attributes
        assertEquals("http", span.getAttributes().get(HTTP_SCHEME)); // http.scheme
        assertEquals(url.getPath() + "span", span.getAttributes().get(HTTP_TARGET)); // http.target
        assertEquals(url.getPath() + "span", span.getAttributes().get(HTTP_ROUTE)); // http.route
        assertNull(span.getAttributes().get(HTTP_CLIENT_IP)); // http.client_ip
        assertEquals(url.getHost(), span.getAttributes().get(NET_HOST_NAME)); // net.host.name
        assertEquals(url.getPort(), span.getAttributes().get(NET_HOST_PORT)); // net.host.port
        assertNotNull(span.getAttributes().get(NET_SOCK_HOST_ADDR)); // net.sock.host.addr
        assertNull(span.getAttributes().get(NET_SOCK_HOST_PORT)); // net.sock.host.port
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
    }

    @ApplicationPath("/")
    public static class RestApplication extends Application {

    }
}
