package io.smallrye.opentelemetry.examples.jaeger;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
