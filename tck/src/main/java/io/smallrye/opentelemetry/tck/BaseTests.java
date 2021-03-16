package io.smallrye.opentelemetry.tck;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
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

import io.grpc.Context;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Span.SpanKind;
import io.opentelemetry.proto.trace.v1.Status.StatusCode;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.TraceId;
import io.opentelemetry.trace.Tracer;
import io.opentelemetry.trace.attributes.SemanticAttributes;
import io.smallrye.opentelemetry.tck.app.TestApplication;
import io.smallrye.opentelemetry.tck.app.TestResource;
import io.smallrye.opentelemetry.tck.app.TracingDisabledResource;

/**
 * Test class for server side JAX-RS tests.
 *
 * @author Pavol Loffay
 */
public class BaseTests extends Arquillian {
    /**
     * OpenTelemetry service port used by tests.
     */
    public static final int OTLP_SERVICE_PORT = 55670;

    private static final int DEFAULT_SLEEP_MS = 1000;

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
        Assert.assertEquals(spans.size(), 1);

        Span span = spans.get(0);
        Assert.assertEquals(span.getKind(), SpanKind.SPAN_KIND_SERVER);
        Assert.assertEquals(span.getStatus().getCode(), StatusCode.STATUS_CODE_OK);
        Assert.assertEquals(span.getParentSpanId().size(), 0);
        Assert.assertEquals(span.getName(),
                String.format("GET:/%s/%s", TestResource.PATH_ROOT, TestResource.PATH_SIMPLE));

        Assert.assertEquals(span.getAttributesCount(), 3);
        Map<String, KeyValue> keyValueAttributeMap = attributeMap(span.getAttributesList());
        assertHttpAttributes(keyValueAttributeMap, uri, "GET", 200);
    }

    @Test
    @RunAsClient
    public void testExceptionInHandler() throws URISyntaxException, MalformedURLException {
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
        Assert.assertEquals(spans.size(), 1);

        Span span = spans.get(0);
        Assert.assertEquals(span.getKind(), SpanKind.SPAN_KIND_SERVER);
        Assert.assertEquals(span.getParentSpanId().size(), 0);
        Assert.assertEquals(span.getStatus().getCode(), StatusCode.STATUS_CODE_UNKNOWN_ERROR);
        Assert.assertEquals(span.getName(),
                String.format("GET:/%s/%s", TestResource.PATH_ROOT, TestResource.PATH_EXCEPTION));

        Assert.assertEquals(span.getAttributesCount(), 3);
        Map<String, KeyValue> keyValueAttributeMap = attributeMap(span.getAttributesList());
        assertHttpAttributes(keyValueAttributeMap, uri, "GET", 500);
    }

    @Test
    @RunAsClient
    public void testAsync() throws URISyntaxException, MalformedURLException {
        Client client = ClientBuilder.newClient();
        URI uri = getURI(deploymentURL,
                TestApplication.PATH_ROOT,
                TestResource.PATH_ROOT,
                TestResource.PATH_ASYNC);
        Response response = client.target(uri)
                .request()
                .get();
        response.close();
        client.close();

        Awaitility.await().until(() -> otlpService.getSpanCount() == 1);
        List<Span> spans = otlpService.getSpans();
        Assert.assertEquals(spans.size(), 1);

        Span span = spans.get(0);
        Assert.assertEquals(span.getKind(), SpanKind.SPAN_KIND_SERVER);
        Assert.assertEquals(span.getParentSpanId().size(), 0);
        Assert.assertEquals(span.getStatus().getCode(), StatusCode.STATUS_CODE_OK);
        Assert.assertEquals(span.getName(),
                String.format("GET:/%s/%s", TestResource.PATH_ROOT, TestResource.PATH_ASYNC));

        Assert.assertEquals(span.getAttributesCount(), 3);
        Map<String, KeyValue> keyValueAttributeMap = attributeMap(span.getAttributesList());
        assertHttpAttributes(keyValueAttributeMap, uri, "GET", 200);
    }

    @Test
    @RunAsClient
    public void testAsyncError() throws URISyntaxException, MalformedURLException {
        Client client = ClientBuilder.newClient();
        URI uri = getURI(deploymentURL,
                TestApplication.PATH_ROOT,
                TestResource.PATH_ROOT,
                TestResource.PATH_ASYNC_ERROR);
        Response response = client.target(uri)
                .request()
                .get();
        response.close();
        client.close();

        Awaitility.await().until(() -> otlpService.getSpanCount() == 1);
        List<Span> spans = otlpService.getSpans();
        Assert.assertEquals(spans.size(), 1);

        Span span = spans.get(0);
        Assert.assertEquals(span.getKind(), SpanKind.SPAN_KIND_SERVER);
        Assert.assertEquals(span.getParentSpanId().size(), 0);
        Assert.assertEquals(span.getStatus().getCode(), StatusCode.STATUS_CODE_UNKNOWN_ERROR);
        Assert.assertEquals(span.getName(),
                String.format("GET:/%s/%s", TestResource.PATH_ROOT, TestResource.PATH_ASYNC_ERROR));

        Assert.assertEquals(span.getAttributesCount(), 3);
        Map<String, KeyValue> keyValueAttributeMap = attributeMap(span.getAttributesList());
        assertHttpAttributes(keyValueAttributeMap, uri, "GET", 500);
    }

    @Test
    @RunAsClient
    private void testTracedFalseMethod()
            throws URISyntaxException, InterruptedException {
        Client client = ClientBuilder.newClient();
        URI uri = getURI(deploymentURL,
                TestApplication.PATH_ROOT,
                TestResource.PATH_ROOT,
                TestResource.PATH_TRACED_FALSE);
        Response response = client.target(uri)
                .request()
                .get();
        response.close();
        client.close();

        Thread.sleep(DEFAULT_SLEEP_MS);
        List<Span> spans = otlpService.getSpans();
        Assert.assertEquals(spans.size(), 0);
    }

    @Test
    @RunAsClient
    private void testTracedFalseClass()
            throws URISyntaxException, InterruptedException {
        Client client = ClientBuilder.newClient();
        URI uri = getURI(deploymentURL,
                TestApplication.PATH_ROOT,
                TracingDisabledResource.PATH_ROOT,
                TracingDisabledResource.PATH_SIMPLE);
        Response response = client.target(uri)
                .request()
                .get();
        response.close();
        client.close();

        Thread.sleep(DEFAULT_SLEEP_MS);
        List<Span> spans = otlpService.getSpans();
        Assert.assertEquals(spans.size(), 0);
    }

    @Test
    @RunAsClient
    private void testTracedFeClassMethodOverrides()
            throws URISyntaxException, MalformedURLException {
        Client client = ClientBuilder.newClient();
        URI uri = getURI(deploymentURL,
                TestApplication.PATH_ROOT,
                TracingDisabledResource.PATH_ROOT,
                TracingDisabledResource.PATH_TRACING_ENABLED);
        Response response = client.target(uri)
                .request()
                .get();
        response.close();
        client.close();

        Awaitility.await().until(() -> otlpService.getSpanCount() == 1);
        List<Span> spans = otlpService.getSpans();
        Assert.assertEquals(spans.size(), 1);

        Span span = spans.get(0);
        Assert.assertEquals(span.getKind(), SpanKind.SPAN_KIND_SERVER);
        Assert.assertEquals(span.getParentSpanId().size(), 0);
        Assert.assertEquals(span.getStatus().getCode(), StatusCode.STATUS_CODE_OK);
        Assert.assertEquals(span.getName(),
                String.format("GET:/%s/%s", TracingDisabledResource.PATH_ROOT, TracingDisabledResource.PATH_TRACING_ENABLED));

        Assert.assertEquals(span.getAttributesCount(), 3);
        Map<String, KeyValue> keyValueAttributeMap = attributeMap(span.getAttributesList());
        assertHttpAttributes(keyValueAttributeMap, uri, "GET", 200);
    }

    @Test
    @RunAsClient
    private void testTracedOverriddenName()
            throws URISyntaxException, MalformedURLException {
        Client client = ClientBuilder.newClient();
        URI uri = getURI(deploymentURL,
                TestApplication.PATH_ROOT,
                TestResource.PATH_ROOT,
                TestResource.PATH_TRACED_OVERRIDE_NAME);
        Response response = client.target(uri)
                .request()
                .get();
        response.close();
        client.close();

        Awaitility.await().until(() -> otlpService.getSpanCount() == 1);
        List<Span> spans = otlpService.getSpans();
        Assert.assertEquals(spans.size(), 1);

        Span span = spans.get(0);
        Assert.assertEquals(span.getKind(), SpanKind.SPAN_KIND_SERVER);
        Assert.assertEquals(span.getParentSpanId().size(), 0);
        Assert.assertEquals(span.getStatus().getCode(), StatusCode.STATUS_CODE_OK);
        Assert.assertEquals(span.getName(), TestResource.TRACED_OVERRIDDEN_NAME);

        Assert.assertEquals(span.getAttributesCount(), 3);
        Map<String, KeyValue> keyValueAttributeMap = attributeMap(span.getAttributesList());
        assertHttpAttributes(keyValueAttributeMap, uri, "GET", 204);
    }

    @Test
    @RunAsClient
    public void testContextPropagation() throws URISyntaxException {
        Client client = ClientBuilder.newClient();
        URI uri = getURI(deploymentURL,
                TestApplication.PATH_ROOT,
                TestResource.PATH_ROOT,
                TestResource.PATH_SIMPLE);
        Builder requestBuilder = client.target(uri)
                .request();

        // Create parent span and inject context into the request headers.
        Tracer tracer = OpenTelemetry.getTracer("io.smallrye.opentelemetry.tck");
        io.opentelemetry.trace.Span parentSpan = tracer.spanBuilder("parent")
                .startSpan();
        try (Scope scope = tracer.withSpan(parentSpan)) {
            OpenTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), requestBuilder,
                    new ClientRequestBuilderTextMapSetter());
        }

        Response response = requestBuilder.get();
        response.close();
        client.close();

        Awaitility.await().until(() -> otlpService.getSpanCount() == 1);
        List<Span> spans = otlpService.getSpans();
        Assert.assertEquals(spans.size(), 1);

        Span span = spans.get(0);
        Assert.assertTrue(span.getParentSpanId().size() > 0);

        SpanId parentSpanId = SpanId.fromBytes(span.getParentSpanId().toByteArray(), 0);
        Assert.assertEquals(parentSpanId, parentSpan.getContext().getSpanId());
        TraceId traceId = TraceId.fromBytes(span.getTraceId().toByteArray(), 0);
        Assert.assertEquals(traceId, parentSpan.getContext().getTraceId());
    }

    @Test
    @RunAsClient
    public void testUrlDoesNotExists()
            throws URISyntaxException, InterruptedException {
        Client client = ClientBuilder.newClient();
        URI uri = getURI(deploymentURL,
                TestApplication.PATH_ROOT,
                TestResource.PATH_ROOT,
                "does_not_exist");
        Response response = client.target(uri)
                .request()
                .get();
        response.close();
        client.close();

        Thread.sleep(DEFAULT_SLEEP_MS);
        List<Span> spans = otlpService.getSpans();
        Assert.assertEquals(spans.size(), 0);
    }

    @Test
    @RunAsClient
    private void testClient() throws URISyntaxException, MalformedURLException {
        Client client = ClientBuilder.newClient();
        Map<String, String> params = new HashMap<String, String>();
        params.put(TestResource.PARAM_NEST_DEPTH, "1");
        URI uri = getURI(deploymentURL,
                params,
                TestApplication.PATH_ROOT,
                TestResource.PATH_ROOT,
                TestResource.PATH_NESTED);
        Response response = client.target(uri)
                .request()
                .get();
        response.close();
        client.close();

        Awaitility.await().until(() -> otlpService.getSpanCount() == 3);
        List<Span> spans = otlpService.getSpans();

        Assert.assertEquals(spans.size(), 3);

        Span span;
        String parentId;
        Map<String, KeyValue> keyValueAttributeMap;

        span = spans.get(2);
        Assert.assertEquals(span.getKind(), SpanKind.SPAN_KIND_SERVER);
        Assert.assertEquals(span.getStatus().getCode(), StatusCode.STATUS_CODE_OK);
        Assert.assertEquals(span.getParentSpanId().size(), 0);
        Assert.assertEquals(span.getName(),
                String.format("GET:/%s/%s", TestResource.PATH_ROOT, TestResource.PATH_NESTED));
        Assert.assertEquals(span.getAttributesCount(), 3);
        keyValueAttributeMap = attributeMap(span.getAttributesList());
        assertHttpAttributes(keyValueAttributeMap, uri, "GET", 200);

        parentId = span.getSpanId().toString(Charset.defaultCharset());
        span = spans.get(1);
        Assert.assertEquals(span.getKind(), SpanKind.SPAN_KIND_CLIENT);
        Assert.assertEquals(span.getStatus().getCode(), StatusCode.STATUS_CODE_OK);
        Assert.assertEquals(span.getParentSpanId().toString(Charset.defaultCharset()), parentId);
        Assert.assertEquals(span.getName(), "GET");

        parentId = span.getSpanId().toString(Charset.defaultCharset());
        span = spans.get(0);
        Assert.assertEquals(span.getKind(), SpanKind.SPAN_KIND_SERVER);
        Assert.assertEquals(span.getStatus().getCode(), StatusCode.STATUS_CODE_OK);
        Assert.assertEquals(span.getParentSpanId().toString(Charset.defaultCharset()), parentId);
        Assert.assertEquals(span.getName(),
                String.format("GET:/%s/%s", TestResource.PATH_ROOT, TestResource.PATH_NESTED));

    }

    private void assertHttpAttributes(Map<String, KeyValue> keyValueAttributeMap, URI uri, String method, int statusCode)
            throws MalformedURLException {
        Assert.assertEquals(keyValueAttributeMap.get(SemanticAttributes.HTTP_METHOD.key()).getValue().getStringValue(), method);
        Assert.assertEquals(keyValueAttributeMap.get(SemanticAttributes.HTTP_STATUS_CODE.key()).getValue().getIntValue(),
                statusCode);
        Assert.assertEquals(keyValueAttributeMap.get(SemanticAttributes.HTTP_URL.key()).getValue().getStringValue(),
                uri.toURL().toString());
    }

    private Map<String, KeyValue> attributeMap(List<KeyValue> attributes) {
        Map<String, KeyValue> keyValueAttributeMap = new LinkedHashMap<>();
        for (KeyValue kv : attributes) {
            keyValueAttributeMap.put(kv.getKey(), kv);
        }
        return keyValueAttributeMap;
    }

    private URI getURI(URL deploymentURL, Map<String, String> params, String... paths) throws URISyntaxException {
        UriBuilder uriBuilder = UriBuilder.fromUri(deploymentURL.toURI());
        for (String path : paths) {
            uriBuilder.path(path);
        }
        for (Map.Entry<String, String> entry : params.entrySet()) {
            uriBuilder.queryParam(entry.getKey(), entry.getValue());
        }
        return uriBuilder.build();
    }

    private URI getURI(URL deploymentURL, String... paths) throws URISyntaxException {
        return getURI(deploymentURL, Collections.emptyMap(), paths);
    }

    private class ClientRequestBuilderTextMapSetter implements TextMapPropagator.Setter<Builder> {
        @Override
        public void set(Builder carrier, String key, String value) {
            carrier.header(key, value);
        }
    }
}
