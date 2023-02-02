package io.smallrye.opentelemetry.test.rest;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;

import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
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

import io.opentelemetry.api.baggage.Baggage;
import io.smallrye.opentelemetry.test.InMemorySpanExporter;

@ExtendWith(ArquillianExtension.class)
class BaggageTest {
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
    void baggage() {
        WebTarget target = ClientBuilder.newClient().target(url.toString() + "baggage");
        Response response = target.request().header("baggage", "user=naruto").get();
        assertEquals(HTTP_OK, response.getStatus());

        spanExporter.getFinishedSpanItems(2);
    }

    @Path("/baggage")
    public static class BaggageResource {
        @Inject
        Baggage baggage;

        @GET
        public Response baggage() {
            assertEquals("naruto", baggage.getEntryValue("user"));
            return Response.ok().build();
        }
    }

    @ApplicationPath("/")
    public static class RestApplication extends Application {

    }
}
