package io.smallrye.opentelemetry.tck.rest;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
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

        SpanData client = spans.get(1);
        assertEquals(CLIENT, client.getKind());

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
