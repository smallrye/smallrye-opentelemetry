package io.smallrye.opentelemetry.implementation.rest;

import static java.util.Collections.emptyList;

import java.lang.reflect.Method;
import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.UriBuilder;

import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesExtractor;

public class RestHttpServerAttributesExtractor
        extends HttpServerAttributesExtractor<ContainerRequestContext, ContainerResponseContext> {

    @Override
    protected String flavor(final ContainerRequestContext request) {
        return null;
    }

    @Override
    protected String target(final ContainerRequestContext request) {
        return null;
    }

    @Override
    protected String route(final ContainerRequestContext request) {
        Class<?> resourceClass = (Class<?>) request.getProperty("rest.resource.class");
        Method method = (Method) request.getProperty("rest.resource.method");

        UriBuilder template = UriBuilder.fromResource(resourceClass);
        if (method.isAnnotationPresent(Path.class)) {
            template.path(method);
        }

        return template.toTemplate();
    }

    @Override
    protected String scheme(final ContainerRequestContext request) {
        return null;
    }

    @Override
    protected String serverName(
            final ContainerRequestContext request,
            final ContainerResponseContext response) {
        return null;
    }

    @Override
    protected String method(final ContainerRequestContext request) {
        return request.getMethod();
    }

    @Override
    protected List<String> requestHeader(final ContainerRequestContext request, final String name) {
        return request.getHeaders().getOrDefault(name, emptyList());
    }

    @Override
    protected Long requestContentLength(final ContainerRequestContext request, final ContainerResponseContext response) {
        return null;
    }

    @Override
    protected Long requestContentLengthUncompressed(final ContainerRequestContext request,
            final ContainerResponseContext response) {
        return null;
    }

    @Override
    protected Integer statusCode(final ContainerRequestContext request, final ContainerResponseContext response) {
        return response.getStatus();
    }

    @Override
    protected Long responseContentLength(final ContainerRequestContext request, final ContainerResponseContext response) {
        return null;
    }

    @Override
    protected Long responseContentLengthUncompressed(final ContainerRequestContext request,
            final ContainerResponseContext response) {
        return null;
    }

    @Override
    protected List<String> responseHeader(final ContainerRequestContext request, final ContainerResponseContext response,
            final String name) {
        return response.getStringHeaders().getOrDefault(name, emptyList());
    }
}
