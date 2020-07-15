package io.smallrye.opentelemetry.jaxrs2.server;

import javax.ws.rs.core.MultivaluedMap;

import io.opentelemetry.context.propagation.HttpTextFormat;

/**
 * Helper class used to iterate over HTTP headers.
 *
 * @author Pavol Loffay
 */
public class ServerHeadersExtractTextMap implements HttpTextFormat.Getter<MultivaluedMap<String, String>> {

    @Override
    public String get(MultivaluedMap<String, String> carrier, String key) {
        return carrier.getFirst(key);
    }
}
