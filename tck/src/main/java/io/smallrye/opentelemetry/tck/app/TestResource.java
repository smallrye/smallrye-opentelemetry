package io.smallrye.opentelemetry.tck.app;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.trace.Tracer;
import io.smallrye.opentelemetry.api.Traced;

/**
 * @author Pavol Loffay
 */
@Path(TestResource.PATH_ROOT)
public class TestResource {

    public static final String PATH_ROOT = "test_resource";
    public static final String PATH_SIMPLE = "simple";
    public static final String PATH_EXCEPTION = "exception";
    public static final String PATH_ASYNC = "async";
    public static final String PATH_ASYNC_ERROR = "async_error";
    public static final String PATH_TRACED_FALSE = "traced_false";
    public static final String PATH_TRACED_OVERRIDE_NAME = "traced_override_name";
    public static final String PATH_NESTED = "nested";

    public static final String PARAM_NEST_DEPTH = "nestDepth";

    public static final String TRACED_OVERRIDDEN_NAME = "overridden_name";

    private static final Tracer TRACER = OpenTelemetry.getTracer("io.smallrye.opentelemetry.tck");

    @Context
    private UriInfo uriInfo;

    @GET
    @Path(PATH_SIMPLE)
    @Produces(MediaType.TEXT_PLAIN)
    public Response simple() {
        assertCurrentSpan();
        return Response.ok().build();
    }

    @GET
    @Path(PATH_EXCEPTION)
    @Produces(MediaType.TEXT_PLAIN)
    public Response exception() {
        assertCurrentSpan();
        throw new RuntimeException("runtime error");
    }

    @GET
    @Path(PATH_ASYNC)
    public void async(@Suspended AsyncResponse asyncResponse) {
        assertCurrentSpan();
        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                asyncResponse.resume(Response.ok().build());
            }
        }).start();
    }

    @GET
    @Path(PATH_ASYNC_ERROR)
    public void asyncError(@Suspended final AsyncResponse asyncResponse) {
        assertCurrentSpan();
        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                // this exception is not propagated to AsyncListener
                asyncResponse.resume(new RuntimeException());
            }
        }).start();
    }

    @GET
    @Traced(value = false)
    @Path(PATH_TRACED_FALSE)
    public void tracedFalse() {
        assertNoCurrentSpan();
    }

    @GET
    @Traced(operationName = TRACED_OVERRIDDEN_NAME)
    @Path(PATH_TRACED_OVERRIDE_NAME)
    public void tracedOverrideName() {
        assertCurrentSpan();
    }

    @GET
    @Path(PATH_NESTED)
    public Response nested(@QueryParam(PARAM_NEST_DEPTH) int nestDepth) throws URISyntaxException {
        if (nestDepth > 0) {
            String newDepth = String.valueOf(nestDepth - 1);
            Map<String, String> params = new HashMap<String, String>();
            params.put(PARAM_NEST_DEPTH, newDepth);
            String requestUrl = getURI(uriInfo.getBaseUri(), params, TestResource.PATH_ROOT, TestResource.PATH_NESTED)
                    .toString();
            executeNested(requestUrl);
        }
        return Response.ok().build();
    }

    private void executeNested(String requestUrl) {
        Client restClient = ClientBuilder.newBuilder().build();
        WebTarget target = restClient.target(requestUrl);
        Response nestedResponse = target.request().get();
        nestedResponse.close();
    }

    public static void assertCurrentSpan() {
        if (!TRACER.getCurrentSpan().isRecording()) {
            throw new AssertionError("Missing current span or is not recording events");
        }
    }

    public static void assertNoCurrentSpan() {
        if (TRACER.getCurrentSpan().isRecording()) {
            throw new AssertionError("Current span should not be preset or recording events");
        }
    }

    private URI getURI(URI uri, Map<String, String> params, String... paths) throws URISyntaxException {
        UriBuilder uriBuilder = UriBuilder.fromUri(uri);
        for (String path : paths) {
            uriBuilder.path(path);
        }
        for (Map.Entry<String, String> entry : params.entrySet()) {
            uriBuilder.queryParam(entry.getKey(), entry.getValue());
        }
        return uriBuilder.build();
    }

}
