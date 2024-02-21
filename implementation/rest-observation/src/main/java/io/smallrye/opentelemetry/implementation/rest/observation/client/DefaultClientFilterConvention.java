package io.smallrye.opentelemetry.implementation.rest.observation.client;

import static io.smallrye.opentelemetry.implementation.rest.observation.FilterDocumentation.HighCardinalityValues;
import static io.smallrye.opentelemetry.implementation.rest.observation.FilterDocumentation.LowCardinalityValues;
import static io.smallrye.opentelemetry.implementation.rest.observation.FilterDocumentation.ClientHighCardinalityValues.CLIENT_ADDRESS;
import static io.smallrye.opentelemetry.implementation.rest.observation.FilterDocumentation.ClientHighCardinalityValues.CLIENT_PORT;
import static io.smallrye.opentelemetry.implementation.rest.observation.ObservationUtil.QUERY_UNKNOWN;
import static io.smallrye.opentelemetry.implementation.rest.observation.ObservationUtil.STATUS_UNKNOWN;
import static io.smallrye.opentelemetry.implementation.rest.observation.ObservationUtil.collectAttribute;
import static io.smallrye.opentelemetry.instrumentation.observation.handler.HandlerUtil.HIGH_CARD_ATTRIBUTES;
import static io.smallrye.opentelemetry.instrumentation.observation.handler.HandlerUtil.LOW_CARD_ATTRIBUTES;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;

// FIXME there's much duplicated code allong with the DefaultServerFilterConvention. Extract common code to a superclass.
public class DefaultClientFilterConvention implements ClientFilterConvention {

    public DefaultClientFilterConvention() {
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ObservationClientContext context) {
        final ClientRequestContext requestContext = context.getCarrier();
        final ClientResponseContext responseContext = context.getResponse();

        // Ideally, we should place data into KeyValues, however they only support String values and OTel uses other types.
        // In order to simplify the setup we will generate data in the final form and place everything
        // in the context for later retrieval in the handlers.
        final AttributesBuilder attributesBuilder = Attributes.builder();
        // Duplicate data to keyValues, otherwise metrics will not be created properly
        final KeyValues lowCardKeyValues = KeyValues.of(
                collectAttribute(
                        attributesBuilder,
                        LowCardinalityValues.HTTP_REQUEST_METHOD,
                        requestContext.getMethod()),
                collectAttribute(
                        attributesBuilder,
                        LowCardinalityValues.URL_SCHEME,
                        requestContext.getUri().getScheme()),
                status(context, attributesBuilder));

        context.put(LOW_CARD_ATTRIBUTES, attributesBuilder.build());
        return lowCardKeyValues;
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ObservationClientContext context) {
        final ClientRequestContext requestContext = context.getCarrier();

        // Ideally, we should place data into KeyValues, however they only support String values and OTel uses other types.
        // In order to simplify the setup we will generate data in the final form and place everything
        // in the context for later retrieval in the handlers.
        final AttributesBuilder attributesBuilder = Attributes.builder();
        // Duplicate data to keyValues, otherwise metrics will not be created properly
        final KeyValues highCardKeyValues = KeyValues.of(
                collectAttribute(attributesBuilder, HighCardinalityValues.URL_PATH, requestContext.getUri().getPath()),
                collectAttribute(attributesBuilder, CLIENT_PORT, Long.valueOf(requestContext.getUri().getPort())),
                collectAttribute(attributesBuilder, CLIENT_ADDRESS, requestContext.getUri().getHost()),
                collectAttribute(attributesBuilder, HighCardinalityValues.URL_FULL, requestContext.getUri().toString()),
                query(context, attributesBuilder));

        context.put(HIGH_CARD_ATTRIBUTES, attributesBuilder.build());
        return highCardKeyValues;
    }

    private KeyValue query(ObservationClientContext context, AttributesBuilder attributesBuilder) {
        final ClientRequestContext requestContext = context.getCarrier();
        final String query = requestContext.getUri().getQuery();
        if (query != null) {
            return collectAttribute(attributesBuilder, HighCardinalityValues.URL_QUERY, query);
        }
        return QUERY_UNKNOWN;
    }

    private KeyValue status(ObservationClientContext context, AttributesBuilder attributesBuilder) {
        if (context.getResponse() != null) {
            return collectAttribute(attributesBuilder,
                    LowCardinalityValues.HTTP_RESPONSE_STATUS_CODE,
                    context.getResponse().getStatus());
        }
        return STATUS_UNKNOWN;
    }

    @Override
    public String getName() {
        return "http.client";
    }

    @Override
    public String getContextualName(ObservationClientContext context) {
        final ClientRequestContext requestContext = context.getCarrier();
        if (requestContext == null) {
            return null;
        }
        return requestContext.getMethod();
    }
}
