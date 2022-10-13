package io.smallrye.opentelemetry.examples.jaeger;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

@Path("/numbers")
@Produces(MediaType.TEXT_PLAIN)
public class NumberResource {
    @Inject
    NumberBean numberBean;
    @Inject
    Tracer tracer;

    @GET
    @Path("/generate")
    public Response generate() {
        Span span = tracer.spanBuilder("span.new")
                .setSpanKind(INTERNAL)
                .setAttribute("tck.new.key", "tck.new.value")
                .startSpan();

        span.end();

        return Response.ok(generateNumber()).build();
    }

    private String generateNumber() {
        return numberBean.getPrefix() + "-" + (int) Math.floor((Math.random() * 9999999)) + 1;
    }
}
