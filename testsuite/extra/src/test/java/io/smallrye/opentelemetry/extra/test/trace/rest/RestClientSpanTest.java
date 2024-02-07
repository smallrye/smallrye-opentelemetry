package io.smallrye.opentelemetry.extra.test.trace.rest;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.SemanticAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.SemanticAttributes.URL_FULL;
import static io.opentelemetry.semconv.SemanticAttributes.URL_PATH;
import static io.opentelemetry.semconv.SemanticAttributes.URL_SCHEME;
import static io.smallrye.opentelemetry.extra.test.AttributeKeysStability.get;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.smallrye.opentelemetry.test.InMemorySpanExporter;

@ExtendWith(ArquillianExtension.class)
class RestClientSpanTest {
    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsResource(new StringAsset("client/mp-rest/url=${baseUri}"), "META-INF/microprofile-config.properties");
    }

    @ArquillianResource
    URL url;
    @Inject
    InMemorySpanExporter spanExporter;
    @Inject
    @RestClient
    SpanResourceClient client;

    @BeforeEach
    void setUp() {
        spanExporter.reset();
    }

    @Test
    void span() {
        Response response = client.span();
        assertEquals(response.getStatus(), HTTP_OK);

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);

        SpanData server = spans.get(0);
        assertEquals(SERVER, server.getKind());
        assertEquals(HttpMethod.GET + " " + url.getPath() + "span", server.getName());
        assertEquals(HTTP_OK, get(server, HTTP_RESPONSE_STATUS_CODE));
        assertEquals(HttpMethod.GET, get(server, HTTP_REQUEST_METHOD));
        assertEquals("http", get(server, URL_SCHEME));
        assertEquals(url.getPath() + "span", get(server, URL_PATH));
        assertEquals(url.getHost(), get(server, SERVER_ADDRESS));

        SpanData client = spans.get(1);
        assertEquals(CLIENT, client.getKind());
        assertEquals("GET", client.getName());
        assertEquals(HTTP_OK, get(server, HTTP_RESPONSE_STATUS_CODE));
        assertEquals(HttpMethod.GET, get(client, HTTP_REQUEST_METHOD));
        assertEquals(url.toString() + "span", get(client, URL_FULL));

        assertEquals(client.getTraceId(), server.getTraceId());
        assertEquals(server.getParentSpanId(), client.getSpanId());
    }

    @Test
    void spanName() {
        Response response = client.spanName("1");
        assertEquals(response.getStatus(), HTTP_OK);

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);

        SpanData server = spans.get(0);
        assertEquals(SERVER, server.getKind());
        assertEquals(HttpMethod.GET + " " + url.getPath() + "span/{name}", server.getName());
        assertEquals(HTTP_OK, get(server, HTTP_RESPONSE_STATUS_CODE));
        assertEquals(HttpMethod.GET, get(server, HTTP_REQUEST_METHOD));
        assertEquals("http", get(server, URL_SCHEME));
        assertEquals(url.getPath() + "span/1", get(server, URL_PATH));
        assertEquals(url.getHost(), get(server, SERVER_ADDRESS));

        SpanData client = spans.get(1);
        assertEquals(CLIENT, client.getKind());
        assertEquals("GET", client.getName());
        assertEquals(HTTP_OK, get(client, HTTP_RESPONSE_STATUS_CODE));
        assertEquals(HttpMethod.GET, get(client, HTTP_REQUEST_METHOD));
        assertEquals(url.toString() + "span/1", get(client, URL_FULL));

        assertEquals(server.getTraceId(), client.getTraceId());
        assertEquals(server.getParentSpanId(), client.getSpanId());
    }

    @Test
    void spanNameQuery() {
        Response response = client.spanNameQuery("1", "query");
        assertEquals(response.getStatus(), HTTP_OK);

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);

        SpanData server = spans.get(0);
        assertEquals(SERVER, server.getKind());
        assertEquals(HttpMethod.GET + " " + url.getPath() + "span/{name}", server.getName());
        assertEquals(HTTP_OK, get(server, HTTP_RESPONSE_STATUS_CODE));
        assertEquals(HttpMethod.GET, get(server, HTTP_REQUEST_METHOD));
        assertEquals("http", get(server, URL_SCHEME));
        assertEquals(url.getPath() + "span/1?query=query", get(server, URL_PATH));
        assertEquals(url.getHost(), get(server, SERVER_ADDRESS));

        SpanData client = spans.get(1);
        assertEquals(CLIENT, client.getKind());
        assertEquals("GET", client.getName());
        assertEquals(HTTP_OK, get(client, HTTP_RESPONSE_STATUS_CODE));
        assertEquals(HttpMethod.GET, get(client, HTTP_REQUEST_METHOD));
        assertEquals(url.toString() + "span/1?query=query", get(client, URL_FULL));

        assertEquals(client.getTraceId(), server.getTraceId());
        assertEquals(server.getParentSpanId(), client.getSpanId());
    }

    @Test
    void spanError() {
        // Can't use REST Client here due to org.jboss.resteasy.microprofile.client.DefaultResponseExceptionMapper
        WebTarget target = ClientBuilder.newClient().target(url.toString() + "span/error");
        Response response = target.request().get();
        assertEquals(response.getStatus(), HTTP_INTERNAL_ERROR);

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);

        SpanData server = spans.get(0);
        assertEquals(SERVER, server.getKind());
        assertEquals(HttpMethod.GET + " " + url.getPath() + "span/error", server.getName());
        assertEquals(HTTP_INTERNAL_ERROR, get(server, HTTP_RESPONSE_STATUS_CODE));
        assertEquals(HttpMethod.GET, get(server, HTTP_REQUEST_METHOD));
        assertEquals("http", get(server, URL_SCHEME));
        assertEquals(url.getPath() + "span/error", get(server, URL_PATH));
        assertEquals(url.getHost(), get(server, SERVER_ADDRESS));

        SpanData client = spans.get(1);
        assertEquals(CLIENT, client.getKind());
        assertEquals("GET", client.getName());
        assertEquals(HTTP_INTERNAL_ERROR, get(client, HTTP_RESPONSE_STATUS_CODE));
        assertEquals(HttpMethod.GET, get(client, HTTP_REQUEST_METHOD));
        assertEquals(url.toString() + "span/error", get(client, URL_FULL));

        assertEquals(client.getTraceId(), server.getTraceId());
        assertEquals(server.getParentSpanId(), client.getSpanId());
    }

    @Test
    void spanChild() {
        Response response = client.spanChild();
        assertEquals(response.getStatus(), HTTP_OK);

        List<SpanData> spans = spanExporter.getFinishedSpanItems(3);

        SpanData internal = spans.get(0);
        assertEquals(INTERNAL, internal.getKind());
        assertEquals("SpanBean.spanChild", internal.getName());

        SpanData server = spans.get(1);
        assertEquals(SERVER, server.getKind());
        assertEquals(HttpMethod.GET + " " + url.getPath() + "span/child", server.getName());
        assertEquals(HTTP_OK, get(server, HTTP_RESPONSE_STATUS_CODE));
        assertEquals(HttpMethod.GET, get(server, HTTP_REQUEST_METHOD));
        assertEquals("http", get(server, URL_SCHEME));
        assertEquals(url.getPath() + "span/child", get(server, URL_PATH));
        assertEquals(url.getHost(), get(server, SERVER_ADDRESS));

        SpanData client = spans.get(2);
        assertEquals(CLIENT, client.getKind());
        assertEquals("GET", client.getName());
        assertEquals(HTTP_OK, get(client, HTTP_RESPONSE_STATUS_CODE));
        assertEquals(HttpMethod.GET, get(client, HTTP_REQUEST_METHOD));
        assertEquals(url.toString() + "span/child", get(client, URL_FULL));

        assertEquals(client.getTraceId(), internal.getTraceId());
        assertEquals(client.getTraceId(), server.getTraceId());
        assertEquals(internal.getParentSpanId(), server.getSpanId());
        assertEquals(server.getParentSpanId(), client.getSpanId());
    }

    @Test
    void spanCurrent() {
        Response response = client.spanCurrent();
        assertEquals(response.getStatus(), HTTP_OK);

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);

        SpanData server = spans.get(0);
        assertEquals(SERVER, server.getKind());
        assertEquals(HttpMethod.GET + " " + url.getPath() + "span/current", server.getName());
        assertEquals(HTTP_OK, get(server, HTTP_RESPONSE_STATUS_CODE));
        assertEquals(HttpMethod.GET, get(server, HTTP_REQUEST_METHOD));
        assertEquals("http", get(server, URL_SCHEME));
        assertEquals(url.getPath() + "span/current", get(server, URL_PATH));
        assertEquals(url.getHost(), get(server, SERVER_ADDRESS));
        assertEquals("tck.current.value", server.getAttributes().get(stringKey("tck.current.key")));

        SpanData client = spans.get(1);
        assertEquals(CLIENT, client.getKind());
        assertEquals("GET", client.getName());
        assertEquals(HTTP_OK, get(client, HTTP_RESPONSE_STATUS_CODE));
        assertEquals(HttpMethod.GET, get(client, HTTP_REQUEST_METHOD));
        assertEquals(url.toString() + "span/current", get(client, URL_FULL));

        assertEquals(client.getTraceId(), server.getTraceId());
        assertEquals(server.getParentSpanId(), client.getSpanId());
    }

    @Test
    void spanNew() {
        Response response = client.spanNew();
        assertEquals(response.getStatus(), HTTP_OK);

        List<SpanData> spans = spanExporter.getFinishedSpanItems(3);

        SpanData internal = spans.get(0);
        assertEquals(INTERNAL, internal.getKind());
        assertEquals("span.new", internal.getName());
        assertEquals("tck.new.value", internal.getAttributes().get(stringKey("tck.new.key")));

        SpanData server = spans.get(1);
        assertEquals(SERVER, server.getKind());
        assertEquals(HttpMethod.GET + " " + url.getPath() + "span/new", server.getName());
        assertEquals(HTTP_OK, get(server, HTTP_RESPONSE_STATUS_CODE));
        assertEquals(HttpMethod.GET, get(server, HTTP_REQUEST_METHOD));
        assertEquals("http", get(server, URL_SCHEME));
        assertEquals(url.getPath() + "span/new", get(server, URL_PATH));
        assertEquals(url.getHost(), get(server, SERVER_ADDRESS));

        SpanData client = spans.get(2);
        assertEquals(CLIENT, client.getKind());
        assertEquals("GET", client.getName());
        assertEquals(HTTP_OK, get(client, HTTP_RESPONSE_STATUS_CODE));
        assertEquals(HttpMethod.GET, get(client, HTTP_REQUEST_METHOD));
        assertEquals(url.toString() + "span/new", get(client, URL_FULL));

        assertEquals(client.getTraceId(), internal.getTraceId());
        assertEquals(client.getTraceId(), server.getTraceId());
        assertEquals(internal.getParentSpanId(), server.getSpanId());
        assertEquals(server.getParentSpanId(), client.getSpanId());
    }

    @RequestScoped
    @Path("/")
    public static class SpanResource {
        @Inject
        SpanBean spanBean;
        @Inject
        Span span;
        @Inject
        Tracer tracer;

        @GET
        @Path("/span")
        public Response span() {
            return Response.ok().build();
        }

        @GET
        @Path("/span/{name}")
        public Response spanName(@PathParam(value = "name") String name, @QueryParam("query") String query) {
            return Response.ok().build();
        }

        @GET
        @Path("/span/error")
        public Response spanError() {
            return Response.serverError().build();
        }

        @GET
        @Path("/span/child")
        public Response spanChild() {
            spanBean.spanChild();
            return Response.ok().build();
        }

        @GET
        @Path("/span/current")
        public Response spanCurrent() {
            span.setAttribute("tck.current.key", "tck.current.value");
            return Response.ok().build();
        }

        @GET
        @Path("/span/new")
        public Response spanNew() {
            Span span = tracer.spanBuilder("span.new")
                    .setSpanKind(INTERNAL)
                    .setParent(Context.current().with(this.span))
                    .setAttribute("tck.new.key", "tck.new.value")
                    .startSpan();

            span.end();

            return Response.ok().build();
        }
    }

    @ApplicationScoped
    public static class SpanBean {
        @WithSpan
        void spanChild() {

        }
    }

    @RegisterRestClient(configKey = "client")
    @Path("/")
    public interface SpanResourceClient {
        @GET
        @Path("/span")
        Response span();

        @GET
        @Path("/span/{name}")
        Response spanName(@PathParam(value = "name") String name);

        @GET
        @Path("/span/{name}")
        Response spanNameQuery(@PathParam(value = "name") String name, @QueryParam("query") String query);

        @GET
        @Path("/span/child")
        Response spanChild();

        @GET
        @Path("/span/current")
        Response spanCurrent();

        @GET
        @Path("/span/new")
        Response spanNew();
    }

    @ApplicationPath("/")
    public static class RestApplication extends Application {

    }
}
