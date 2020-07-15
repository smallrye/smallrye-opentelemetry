package io.smallrye.opentelemetry.example.resteasy;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * @author Pavol Loffay
 */
@Path("/")
public class ExampleResource {

    @GET
    @Path("/")
    public String hello() {
        return "hello";
    }

    @GET
    @Path("/error")
    public void error() {
        throw new RuntimeException("some error");
    }
}
