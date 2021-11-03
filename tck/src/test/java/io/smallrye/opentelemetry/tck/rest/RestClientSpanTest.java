package io.smallrye.opentelemetry.tck.rest;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_HOST;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_SCHEME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_SERVER_NAME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_TARGET;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_URL;
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

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.smallrye.opentelemetry.tck.InMemorySpanExporter;

@ExtendWith(ArquillianExtension.class)
public class RestClientSpanTest {
    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsResource(new StringAsset("client/mp-rest/url=${baseUri}"), "META-INF/microprofile-config.properties");
    }

    InMemorySpanExporter spanExporter;

    @ArquillianResource
    private URL url;
    @Inject
    @RestClient
    SpanResourceClient client;

    @BeforeEach
    void setUp() {
        spanExporter = InMemorySpanExporter.HOLDER.get();
        spanExporter.reset();
    }

    @Test
    void span() {
        Response response = client.span();
        assertEquals(response.getStatus(), HTTP_OK);

        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertEquals(2, spans.size());

        SpanData server = spans.get(0);
        assertEquals(SERVER, server.getKind());
        assertEquals(url.getPath() + "span", server.getName());
        assertEquals(HTTP_OK, server.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, server.getAttributes().get(HTTP_METHOD));
        assertEquals("http", server.getAttributes().get(HTTP_SCHEME));
        assertEquals(url.getHost(), server.getAttributes().get(HTTP_SERVER_NAME));
        assertEquals(url.getHost() + ":" + url.getPort(), server.getAttributes().get(HTTP_HOST));
        assertEquals(url.getPath() + "span", server.getAttributes().get(HTTP_TARGET));

        SpanData client = spans.get(1);
        assertEquals(CLIENT, client.getKind());
        assertEquals("HTTP GET", client.getName());
        assertEquals(HTTP_OK, client.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, client.getAttributes().get(HTTP_METHOD));
        assertEquals(url.toString() + "span", client.getAttributes().get(HTTP_URL));

        assertEquals(server.getTraceId(), client.getTraceId());
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

    @RegisterRestClient(configKey = "client")
    @Path("/")
    public interface SpanResourceClient {
        @GET
        @Path("/span")
        Response span();
    }

    @ApplicationPath("/")
    public static class RestApplication extends Application {

    }
}
