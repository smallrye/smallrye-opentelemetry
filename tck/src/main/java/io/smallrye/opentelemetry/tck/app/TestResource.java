package io.smallrye.opentelemetry.tck.app;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

    public static final String TRACED_OVERRIDDEN_NAME = "overridden_name";

    private static final Tracer TRACER = OpenTelemetry.getTracer("io.smallrye.opentelemetry.tck");

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
        assertCurrentSpan();
    }

    @GET
    @Traced(operationName = TRACED_OVERRIDDEN_NAME)
    @Path(PATH_TRACED_OVERRIDE_NAME)
    public void tracedOverrideName() {
        assertCurrentSpan();
    }

    private void assertCurrentSpan() {
        if (!TRACER.getCurrentSpan().isRecording()) {
            throw new AssertionError("Mussing current span");
        }
    }
}
