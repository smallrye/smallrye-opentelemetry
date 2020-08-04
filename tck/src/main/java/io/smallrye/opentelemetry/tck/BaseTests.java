package io.smallrye.opentelemetry.tck;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

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
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Span.SpanKind;
// import io.opentelemetry.proto.trace.v1.Status.StatusCode;
import io.opentelemetry.trace.attributes.SemanticAttributes;
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
    private void testStandardTags() throws URISyntaxException, MalformedURLException {
        Client client = ClientBuilder.newClient();
        URI uri = getURI(deploymentURL,
                TestApplication.PATH_ROOT,
                TestResource.PATH_ROOT,
                TestResource.PATH_SIMPLE);
        Response response = client.target(uri)
                .request()
                .get();
        response.close();
        client.close();

        Awaitility.await().until(() -> otlpService.getSpanCount() == 1);
        List<Span> spans = otlpService.getSpans();
        Assert.assertEquals(1, spans.size());

        Span span = spans.get(0);
        Assert.assertEquals(span.getKind(), SpanKind.SERVER);
        //        Assert.assertEquals(span.getStatus().getCode(), StatusCode.Ok);
        //        Assert.assertEquals(span.getLinksCount(), 0);
        Assert.assertEquals(span.getName(),
                String.format("GET:/%s/%s", TestResource.PATH_ROOT, TestResource.PATH_SIMPLE));
        Assert.assertEquals(span.getAttributesCount(), 3);
        Map<String, KeyValue> keyValueAttributeMat = attributeMap(span.getAttributesList());
        Assert.assertEquals(keyValueAttributeMat.get(SemanticAttributes.HTTP_METHOD.key()).getValue().getStringValue(), "GET");
        Assert.assertEquals(keyValueAttributeMat.get(SemanticAttributes.HTTP_STATUS_CODE.key()).getValue().getIntValue(), 200);
        Assert.assertEquals(keyValueAttributeMat.get(SemanticAttributes.HTTP_URL.key()).getValue().getStringValue(),
                uri.toURL().toString());
    }

    @Test
    public void testExceptionInHandler() throws URISyntaxException {
        Client client = ClientBuilder.newClient();
        URI uri = getURI(deploymentURL,
                TestApplication.PATH_ROOT,
                TestResource.PATH_ROOT,
                TestResource.PATH_EXCEPTION);
        Response response = client.target(uri)
                .request()
                .get();
        response.close();
        client.close();

        Awaitility.await().until(() -> otlpService.getSpanCount() == 1);
        List<Span> spans = otlpService.getSpans();
        Assert.assertEquals(1, spans.size());

        Span span = spans.get(0);
        Assert.assertEquals(span.getKind(), SpanKind.SERVER);
        Assert.assertEquals(span.getLinksCount(), 0);
        //        Assert.assertEquals(span.getStatus().getCode(), StatusCode.UnknownError);
        Assert.assertEquals(span.getName(),
                String.format("GET:/%s/%s", TestResource.PATH_ROOT, TestResource.PATH_EXCEPTION));

        //        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        //        Assert.assertEquals(1, mockSpans.size());
        //        MockSpan mockSpan = mockSpans.get(0);
        //        Assert.assertEquals(6, mockSpan.tags().size());
        //        Assert.assertEquals(true, mockSpan.tags().get(Tags.ERROR.getKey()));
        //        Assert.assertEquals(1, mockSpan.logEntries().size());
        //        Assert.assertEquals(2, mockSpan.logEntries().get(0).fields().size());
        //        Assert.assertNotNull(mockSpan.logEntries().get(0).fields().get("error.object"));
        //        Assert.assertEquals("error", mockSpan.logEntries().get(0).fields().get("event"));
        // TODO resteasy and CXF returns 200
        // Resteasy filter https://issues.jboss.org/browse/RESTEASY-1758
        //        Assert.assertEquals(500, mockSpan.tags().get(Tags.HTTP_STATUS.getKey()));
    }

    private Map<String, KeyValue> attributeMap(List<KeyValue> attributes) {
        Map<String, KeyValue> keyValueAttributeMap = new LinkedHashMap<>();
        for (KeyValue kv : attributes) {
            keyValueAttributeMap.put(kv.getKey(), kv);
        }
        return keyValueAttributeMap;
    }

    private URI getURI(URL deploymentURL, String... paths) throws URISyntaxException {
        UriBuilder uriBuilder = UriBuilder.fromUri(deploymentURL.toURI());
        for (String path : paths) {
            uriBuilder.path(path);
        }
        return uriBuilder.build();
    }
}
