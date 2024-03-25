package io.smallrye.opentelemetry.implementation.rest.observation.client;

import jakarta.ws.rs.client.ClientRequestContext;

import io.micrometer.common.KeyValues;
import io.smallrye.opentelemetry.implementation.rest.observation.FilterDocumentation;

// FIXME there's much duplicated code allong with the DefaultServerFilterConvention. Extract common code to a superclass.
public class DefaultClientFilterConvention implements ClientFilterConvention {

    public DefaultClientFilterConvention() {
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ObservationClientContext context) {
        final ClientRequestContext requestContext = context.getRequestContext();
        return KeyValues.of(
                FilterDocumentation.LowCardinalityValues.HTTP_REQUEST_METHOD.withValue(requestContext.getMethod()),
                FilterDocumentation.LowCardinalityValues.URL_PATH.withValue(requestContext.getUri().getPath()),
                FilterDocumentation.LowCardinalityValues.URL_SCHEME.withValue(requestContext.getUri().getScheme()),
                FilterDocumentation.ClientLowCardinalityValues.CLIENT_PORT.withValue("" + requestContext.getUri().getPort()),
                FilterDocumentation.ClientLowCardinalityValues.CLIENT_ADDRESS.withValue(requestContext.getUri().getHost()));
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ObservationClientContext context) {
        final ClientRequestContext requestContext = context.getRequestContext();
        return KeyValues.of(
                FilterDocumentation.HighCardinalityValues.URL_QUERY.withValue(requestContext.getUri().getQuery()),
                FilterDocumentation.HighCardinalityValues.URL_FULL.withValue(requestContext.getUri().toString()));
    }

    @Override
    public String getName() {
        return "http.client";
    }

    @Override
    public String getContextualName(ObservationClientContext context) {
        final ClientRequestContext requestContext = context.getRequestContext();
        if (requestContext == null) {
            return null;
        }
        return requestContext.getMethod();
    }
}
