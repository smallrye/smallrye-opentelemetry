package io.smallrye.opentelemetry.extra.test.logs;

import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.smallrye.opentelemetry.test.InMemoryExporter;

@ExtendWith(ArquillianExtension.class)
public class LogsTest {
    @Inject
    InMemoryExporter logExporter;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsResource(new StringAsset("otel.logs.exporter=in-memory"), "META-INF/microprofile-config.properties");
    }

    @BeforeEach
    void setUp() {
        logExporter.reset();
    }

    @Test
    void exporter() {
        assertNotNull(logExporter);
    }

    @Test
    void logMessage() throws InterruptedException {
        given().get("/log").then().statusCode(HTTP_OK);

        List<LogRecordData> items = logExporter.getFinishedLogRecordItems(1);

        assertTrue(items.stream().anyMatch(r -> {
            var value = r.getBodyValue();
            return value != null && value.asString().contains("Test message");
        }), "Log message not found");
    }

    @Path("/")
    public static class LogResource {
        private static final Logger logger = Logger.getLogger(LogResource.class.getName());

        @Inject
        Tracer tracer;

        @GET
        @Path("/log")
        public Response log() {
            logger.log(Level.INFO, "Test %s", "message");
            return Response.ok().build();
        }
    }

    @ApplicationPath("/")
    public static class RestApplication extends Application {

    }
}
