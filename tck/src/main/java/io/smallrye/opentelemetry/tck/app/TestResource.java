package io.smallrye.opentelemetry.tck.app;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author Pavol Loffay
 */
@Path(TestResource.PATH_ROOT)
public class TestResource {

    public static final String PATH_ROOT = "test_resource";

    public static final String PATH_SIMPLE = "simple";

    @GET
    @Path(PATH_SIMPLE)
    @Produces(MediaType.TEXT_PLAIN)
    public void simple() {
    }
}
