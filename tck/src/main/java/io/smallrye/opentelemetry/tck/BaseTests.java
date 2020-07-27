package io.smallrye.opentelemetry.tck;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.awaitility.Awaitility;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.opentelemetry.proto.trace.v1.Span;
import io.smallrye.opentelemetry.tck.app.TestApplication;
import io.smallrye.opentelemetry.tck.app.TestResource;

/**
 * @author Pavol Loffay
 */
public class BaseTests extends Arquillian {
    /**
     * OpenTelemetry service port used by tests.
     */
    public static final int OTLP_SERVICE_PORT = 55670;

    /**
     * Server app URL for the client tests.
     */
    @ArquillianResource
    protected URL deploymentURL;

    private static Server server;
    private static OTLPService otlpService;

    /**
     * Deploy the apps to test.
     *
     * @return the Deployed apps
     */
    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "opentelemetry.war")
                .addPackages(true, BaseTests.class.getPackage())
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @BeforeClass
    private static void startOTLPGRPCService() throws IOException {
        otlpService = new OTLPService();
        server = ServerBuilder.forPort(OTLP_SERVICE_PORT)
                .addService(otlpService)
                .build()
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                stopOTLPGRPCService();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
        }));
    }

    private static void stopOTLPGRPCService() throws InterruptedException {
        if (server != null && !server.isShutdown()) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    @BeforeMethod
    public void reset() {
        if (otlpService != null) {
            otlpService.reset();
        }
    }

    /**
     * Test server-side JAX-RS endpoint.
     */
    @Test
    @RunAsClient
    private void testServerEndpoint() throws URISyntaxException {
        Client client = ClientBuilder.newClient();
        Response response = client.target(deploymentURL.toURI())
                .path(TestApplication.PATH_ROOT)
                .path(TestResource.PATH_ROOT)
                .path(TestResource.PATH_SIMPLE)
                .request()
                .get();
        response.close();
        client.close();

        Awaitility.await().until(() -> otlpService.getSpanCount() == 1);
        List<Span> spans = otlpService.getSpans();
        Assert.assertEquals(1, spans.size());
        Assert.assertEquals(spans.get(0).getName(),
                String.format("GET:/%s/%s", TestResource.PATH_ROOT, TestResource.PATH_SIMPLE));
    }
}
