package io.smallrye.opentelemetry.tck.app;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.smallrye.opentelemetry.api.Traced;

/**
 * @author Pavol Loffay
 */
@Traced(value = false)
@Path(TracingDisabledResource.PATH_ROOT)
public class TracingDisabledResource {

    public static final String PATH_ROOT = "tracing_disabled";
    public static final String PATH_SIMPLE = "simple";
    public static final String PATH_TRACING_ENABLED = "tracing_enabled";

    @GET
    @Path(PATH_SIMPLE)
    @Produces(MediaType.TEXT_PLAIN)
    public Response simple() {
        TestResource.assertNoCurrentSpan();
        return Response.ok().build();
    }

    @Traced(value = true)
    @GET
    @Path(PATH_TRACING_ENABLED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response tracingEnabled() {
        TestResource.assertCurrentSpan();
        return Response.ok().build();
    }
}
