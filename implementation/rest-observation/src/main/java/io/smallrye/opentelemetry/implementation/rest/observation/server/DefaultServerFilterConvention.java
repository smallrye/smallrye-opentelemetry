package io.smallrye.opentelemetry.implementation.rest.observation.server;

import java.lang.reflect.Method;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriBuilder;

import io.micrometer.common.KeyValues;
import io.smallrye.opentelemetry.implementation.rest.observation.FilterDocumentation;

public class DefaultServerFilterConvention implements ServerFilterConvention {

    public DefaultServerFilterConvention() {
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ObservationServerContext context) {
        final ContainerRequestContext requestContext = context.getRequestContext();
        KeyValues keyValues = KeyValues.of(
                FilterDocumentation.LowCardinalityValues.HTTP_REQUEST_METHOD.withValue(requestContext.getMethod()),
                FilterDocumentation.LowCardinalityValues.URL_PATH.withValue(requestContext.getUriInfo().getPath()),
                FilterDocumentation.LowCardinalityValues.HTTP_ROUTE.withValue(getHttpRoute(context)),
                FilterDocumentation.LowCardinalityValues.URL_SCHEME
                        .withValue(requestContext.getUriInfo().getRequestUri().getScheme()),
                FilterDocumentation.ServerLowCardinalityValues.SERVER_PORT
                        .withValue("" + requestContext.getUriInfo().getRequestUri().getPort()),
                FilterDocumentation.ServerLowCardinalityValues.SERVER_ADDRESS
                        .withValue(requestContext.getUriInfo().getRequestUri().getHost()));

        if (context.getResponseContext() != null) {
            keyValues.and(
                    FilterDocumentation.LowCardinalityValues.HTTP_RESPONSE_STATUS_CODE
                            .withValue("" + context.getResponseContext().getStatus()));
        }
        return keyValues;
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ObservationServerContext context) {
        final ContainerRequestContext requestContext = context.getRequestContext();
        return KeyValues.of(
                FilterDocumentation.HighCardinalityValues.URL_QUERY
                        .withValue(requestContext.getUriInfo().getRequestUri().getQuery()));
    }

    @Override
    public String getName() {
        return "http.server";
    }

    @Override
    public String getContextualName(ObservationServerContext context) {
        final ContainerRequestContext requestContext = context.getRequestContext();
        final String route = getHttpRoute(context);
        return route == null ? requestContext.getMethod() : requestContext.getMethod() + " " + route;
    }

    private String getHttpRoute(final ObservationServerContext request) {
        try {
            // This can throw an IllegalArgumentException when determining the route for a subresource
            Class<?> resource = (Class<?>) request.getResourceInfo().getResourceClass();
            Method method = (Method) request.getResourceInfo().getResourceMethod();

            UriBuilder uriBuilder = UriBuilder.newInstance();
            String contextRoot = request.getRequestContext().getUriInfo().getBaseUri().getPath();
            if (contextRoot != null) {
                uriBuilder.path(contextRoot);
            }
            uriBuilder.path(resource);
            if (method.isAnnotationPresent(Path.class)) {
                uriBuilder.path(method);
            }

            return uriBuilder.toTemplate();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
