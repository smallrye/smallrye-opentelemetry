package io.smallrye.opentelemetry.examples.agent;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/numbers")
@Produces(MediaType.TEXT_PLAIN)
public class NumberResource {
    @Inject
    NumberBean numberBean;

    @GET
    @Path("/generate")
    public Response generate() {
        return Response.ok(generateNumber()).build();
    }

    private String generateNumber() {
        return numberBean.getPrefix() + "-" + (int) Math.floor((Math.random() * 9999999)) + 1;
    }
}
