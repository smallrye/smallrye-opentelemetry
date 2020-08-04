package io.smallrye.opentelemetry.tck.app;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Pavol Loffay
 */
@Path(TestResource.PATH_ROOT)
public class TestResource {

    public static final String PATH_ROOT = "test_resource";
    public static final String PATH_SIMPLE = "simple";
    public static final String PATH_EXCEPTION = "exception";

    @GET
    @Path(PATH_SIMPLE)
    @Produces(MediaType.TEXT_PLAIN)
    public Response simple() {
        return Response.ok().build();
    }

    @GET
    @Path(PATH_EXCEPTION)
    @Produces(MediaType.TEXT_PLAIN)
    public Response exception() {
        throw new RuntimeException();
    }
}
