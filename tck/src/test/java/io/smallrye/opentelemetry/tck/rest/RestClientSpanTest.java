package io.smallrye.opentelemetry.tck.rest;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_SCHEME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_TARGET;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_URL;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_NAME;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.smallrye.opentelemetry.tck.InMemorySpanExporter;

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
        assertEquals(url.getPath() + "span", server.getName());
        assertEquals(HTTP_OK, server.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, server.getAttributes().get(HTTP_METHOD));
        assertEquals("http", server.getAttributes().get(HTTP_SCHEME));
        assertEquals(url.getPath() + "span", server.getAttributes().get(HTTP_TARGET));
        assertEquals(url.getHost(), server.getAttributes().get(NET_HOST_NAME));

        SpanData client = spans.get(1);
        assertEquals(CLIENT, client.getKind());
        assertEquals("HTTP GET", client.getName());
        assertEquals(HTTP_OK, client.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, client.getAttributes().get(HTTP_METHOD));
        assertEquals(url.toString() + "span", client.getAttributes().get(HTTP_URL));

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
        assertEquals(url.getPath() + "span/{name}", server.getName());
        assertEquals(HTTP_OK, server.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, server.getAttributes().get(HTTP_METHOD));
        assertEquals("http", server.getAttributes().get(HTTP_SCHEME));
        assertEquals(url.getPath() + "span/1", server.getAttributes().get(HTTP_TARGET));
        assertEquals(url.getHost(), server.getAttributes().get(NET_HOST_NAME));

        SpanData client = spans.get(1);
        assertEquals(CLIENT, client.getKind());
        assertEquals("HTTP GET", client.getName());
        assertEquals(HTTP_OK, client.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, client.getAttributes().get(HTTP_METHOD));
        assertEquals(url.toString() + "span/1", client.getAttributes().get(HTTP_URL));

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
        assertEquals(url.getPath() + "span/{name}", server.getName());
        assertEquals(HTTP_OK, server.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, server.getAttributes().get(HTTP_METHOD));
        assertEquals("http", server.getAttributes().get(HTTP_SCHEME));
        assertEquals(url.getPath() + "span/1?query=query", server.getAttributes().get(HTTP_TARGET));
        assertEquals(url.getHost(), server.getAttributes().get(NET_HOST_NAME));

        SpanData client = spans.get(1);
        assertEquals(CLIENT, client.getKind());
        assertEquals("HTTP GET", client.getName());
        assertEquals(HTTP_OK, client.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, client.getAttributes().get(HTTP_METHOD));
        assertEquals(url.toString() + "span/1?query=query", client.getAttributes().get(HTTP_URL));

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
        assertEquals(url.getPath() + "span/error", server.getName());
        assertEquals(HTTP_INTERNAL_ERROR, server.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, server.getAttributes().get(HTTP_METHOD));
        assertEquals("http", server.getAttributes().get(HTTP_SCHEME));
        assertEquals(url.getPath() + "span/error", server.getAttributes().get(HTTP_TARGET));
        assertEquals(url.getHost(), server.getAttributes().get(NET_HOST_NAME));

        SpanData client = spans.get(1);
        assertEquals(CLIENT, client.getKind());
        assertEquals("HTTP GET", client.getName());
        assertEquals(HTTP_INTERNAL_ERROR, client.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, client.getAttributes().get(HTTP_METHOD));
        assertEquals(url.toString() + "span/error", client.getAttributes().get(HTTP_URL));

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
        assertEquals(url.getPath() + "span/child", server.getName());
        assertEquals(HTTP_OK, server.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, server.getAttributes().get(HTTP_METHOD));
        assertEquals("http", server.getAttributes().get(HTTP_SCHEME));
        assertEquals(url.getPath() + "span/child", server.getAttributes().get(HTTP_TARGET));
        assertEquals(url.getHost(), server.getAttributes().get(NET_HOST_NAME));

        SpanData client = spans.get(2);
        assertEquals(CLIENT, client.getKind());
        assertEquals("HTTP GET", client.getName());
        assertEquals(HTTP_OK, client.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, client.getAttributes().get(HTTP_METHOD));
        assertEquals(url.toString() + "span/child", client.getAttributes().get(HTTP_URL));

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
        assertEquals(url.getPath() + "span/current", server.getName());
        assertEquals(HTTP_OK, server.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, server.getAttributes().get(HTTP_METHOD));
        assertEquals("http", server.getAttributes().get(HTTP_SCHEME));
        assertEquals(url.getPath() + "span/current", server.getAttributes().get(HTTP_TARGET));
        assertEquals(url.getHost(), server.getAttributes().get(NET_HOST_NAME));
        assertEquals("tck.current.value", server.getAttributes().get(stringKey("tck.current.key")));

        SpanData client = spans.get(1);
        assertEquals(CLIENT, client.getKind());
        assertEquals("HTTP GET", client.getName());
        assertEquals(HTTP_OK, client.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, client.getAttributes().get(HTTP_METHOD));
        assertEquals(url.toString() + "span/current", client.getAttributes().get(HTTP_URL));

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
        assertEquals(url.getPath() + "span/new", server.getName());
        assertEquals(HTTP_OK, server.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, server.getAttributes().get(HTTP_METHOD));
        assertEquals("http", server.getAttributes().get(HTTP_SCHEME));
        assertEquals(url.getPath() + "span/new", server.getAttributes().get(HTTP_TARGET));
        assertEquals(url.getHost(), server.getAttributes().get(NET_HOST_NAME));

        SpanData client = spans.get(2);
        assertEquals(CLIENT, client.getKind());
        assertEquals("HTTP GET", client.getName());
        assertEquals(HTTP_OK, client.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, client.getAttributes().get(HTTP_METHOD));
        assertEquals(url.toString() + "span/new", client.getAttributes().get(HTTP_URL));

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
