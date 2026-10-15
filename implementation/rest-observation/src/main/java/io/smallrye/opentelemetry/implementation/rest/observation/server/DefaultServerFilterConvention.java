package io.smallrye.opentelemetry.implementation.rest.observation.server;

import static io.smallrye.opentelemetry.implementation.rest.observation.FilterDocumentation.LowCardinalityValues;
import static io.smallrye.opentelemetry.implementation.rest.observation.FilterDocumentation.ServerHighCardinalityValues;
import static io.smallrye.opentelemetry.implementation.rest.observation.ObservationUtil.QUERY_UNKNOWN;
import static io.smallrye.opentelemetry.implementation.rest.observation.ObservationUtil.STATUS_UNKNOWN;
import static io.smallrye.opentelemetry.implementation.rest.observation.ObservationUtil.UNKNOWN;
import static io.smallrye.opentelemetry.implementation.rest.observation.ObservationUtil.USER_AGENT_UNKNOWN;
import static io.smallrye.opentelemetry.implementation.rest.observation.ObservationUtil.collectAttribute;
import static io.smallrye.opentelemetry.instrumentation.observation.handler.HandlerUtil.HIGH_CARD_ATTRIBUTES;
import static io.smallrye.opentelemetry.instrumentation.observation.handler.HandlerUtil.LOW_CARD_ATTRIBUTES;

import java.lang.reflect.Method;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriBuilder;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.smallrye.opentelemetry.implementation.rest.observation.FilterDocumentation.HighCardinalityValues;

public class DefaultServerFilterConvention implements ServerFilterConvention {

    public DefaultServerFilterConvention() {
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ObservationServerContext context) {
        final ContainerRequestContext requestContext = context.getCarrier();

        // Ideally, we should place data into KeyValues, however they only support String values and OTel uses other types.
        // In order to simplify the setup we will generate data in the final form and place everything
        // in the context for later retrieval in the handlers.
        final AttributesBuilder attributesBuilder = Attributes.builder();
        // Duplicate data to keyValues, otherwise metrics will not be created properly
        final KeyValues lowCardKeyValues = KeyValues.of(
                collectAttribute(attributesBuilder, LowCardinalityValues.HTTP_REQUEST_METHOD, requestContext.getMethod()),
                collectAttribute(attributesBuilder, LowCardinalityValues.HTTP_ROUTE, getHttpRoute(context)),
                collectAttribute(attributesBuilder, LowCardinalityValues.URL_SCHEME,
                        requestContext.getUriInfo().getRequestUri().getScheme()),
                collectAttribute(attributesBuilder, LowCardinalityValues.NETWORK_PROTOCOL_NAME, "http"),
                collectAttribute(attributesBuilder, LowCardinalityValues.NETWORK_PROTOCOL_VERSION, UNKNOWN),
                status(context, attributesBuilder));

        context.put(LOW_CARD_ATTRIBUTES, attributesBuilder.build());
        return lowCardKeyValues;
    }

    private KeyValue status(ObservationServerContext context, AttributesBuilder attributesBuilder) {
        if (context.getResponse() != null) {
            return collectAttribute(attributesBuilder,
                    LowCardinalityValues.HTTP_RESPONSE_STATUS_CODE,
                    context.getResponse().getStatus());
        }
        return STATUS_UNKNOWN;
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ObservationServerContext context) {
        final ContainerRequestContext requestContext = context.getCarrier();
        // Ideally, we should place data into KeyValues, however they only support String values and OTel uses other types.
        // In order to simplify the setup we will generate data in the final form and place everything
        // in the context for later retrieval in the handlers.
        final AttributesBuilder attributesBuilder = Attributes.builder();
        // Duplicate data to keyValues, otherwise metrics will not be created properly
        final KeyValues highCardKeyValues = KeyValues.of(
                collectAttribute(
                        attributesBuilder,
                        HighCardinalityValues.URL_PATH,
                        requestContext.getUriInfo().getRequestUri().getPath()),
                collectAttribute(
                        attributesBuilder,
                        ServerHighCardinalityValues.SERVER_PORT,
                        Long.valueOf(requestContext.getUriInfo().getRequestUri().getPort())),
                collectAttribute(
                        attributesBuilder,
                        ServerHighCardinalityValues.SERVER_ADDRESS,
                        requestContext.getUriInfo().getRequestUri().getHost()),
                agent(context, attributesBuilder),
                urlQuery(context, attributesBuilder));

        context.put(HIGH_CARD_ATTRIBUTES, attributesBuilder.build());
        return highCardKeyValues;
    }

    private KeyValue urlQuery(ObservationServerContext context, AttributesBuilder attributesBuilder) {
        if (context.getCarrier().getUriInfo().getRequestUri().getQuery() != null) {
            context.getCarrier().getUriInfo().getRequestUri().getQuery();
            return collectAttribute(
                    attributesBuilder,
                    HighCardinalityValues.URL_QUERY,
                    context.getCarrier().getUriInfo().getRequestUri().getQuery());
        }
        return QUERY_UNKNOWN;
    }

    private KeyValue agent(ObservationServerContext context, AttributesBuilder attributesBuilder) {
        String userAgent = extractUserAgent(context.getCarrier());
        if (userAgent != null) {
            return collectAttribute(
                    attributesBuilder,
                    HighCardinalityValues.USER_AGENT_ORIGINAL,
                    userAgent);
        }
        return USER_AGENT_UNKNOWN;
    }

    @Override
    public String getName() {
        return "http.server";
    }

    @Override
    public String getContextualName(ObservationServerContext context) {
        final ContainerRequestContext requestContext = context.getCarrier();
        final String route = getHttpRoute(context);
        return route == null ? requestContext.getMethod() : requestContext.getMethod() + " " + route;
    }

    private String getHttpRoute(final ObservationServerContext request) {
        try {
            // This can throw an IllegalArgumentException when determining the route for a subresource
            Class<?> resource = (Class<?>) request.getResourceInfo().getResourceClass();
            Method method = (Method) request.getResourceInfo().getResourceMethod();

            UriBuilder uriBuilder = UriBuilder.newInstance();
            String contextRoot = request.getCarrier().getUriInfo().getBaseUri().getPath();
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

    public String extractUserAgent(ContainerRequestContext requestContext) {
        String userAgent = requestContext.getHeaderString(HttpHeaders.USER_AGENT);
        if (userAgent != null) {
            return userAgent;
        } else {
            return null;
        }
    }
}
