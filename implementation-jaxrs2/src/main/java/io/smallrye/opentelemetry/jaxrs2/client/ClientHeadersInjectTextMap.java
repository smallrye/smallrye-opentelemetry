package io.smallrye.opentelemetry.jaxrs2.client;

import javax.ws.rs.core.MultivaluedMap;

import io.opentelemetry.context.propagation.TextMapPropagator;

/**
 * Helper class used to set HTTP headers.
 *
 * @author Felix Wong
 */
public class ClientHeadersInjectTextMap implements TextMapPropagator.Setter<MultivaluedMap<String, Object>> {

    @Override
    public void set(MultivaluedMap<String, Object> carrier, String key, String value) {
        carrier.add(key, value);
    }
}
