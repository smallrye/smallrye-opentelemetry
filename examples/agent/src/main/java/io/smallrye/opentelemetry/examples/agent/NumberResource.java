package io.smallrye.opentelemetry.examples.agent;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
