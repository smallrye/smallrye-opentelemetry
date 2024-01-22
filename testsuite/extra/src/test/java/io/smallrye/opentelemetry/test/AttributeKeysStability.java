package io.smallrye.opentelemetry.test;

import static io.opentelemetry.semconv.SemanticAttributes.CLIENT_ADDRESS;
import static io.opentelemetry.semconv.SemanticAttributes.CLIENT_SOCKET_ADDRESS;
import static io.opentelemetry.semconv.SemanticAttributes.CLIENT_SOCKET_PORT;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_CLIENT_IP;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_REQUEST_BODY_SIZE;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_RESPONSE_BODY_SIZE;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_SCHEME;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_TARGET;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_URL;
import static io.opentelemetry.semconv.SemanticAttributes.NETWORK_PROTOCOL_NAME;
import static io.opentelemetry.semconv.SemanticAttributes.NETWORK_PROTOCOL_VERSION;
import static io.opentelemetry.semconv.SemanticAttributes.NET_HOST_NAME;
import static io.opentelemetry.semconv.SemanticAttributes.NET_HOST_PORT;
import static io.opentelemetry.semconv.SemanticAttributes.NET_PROTOCOL_NAME;
import static io.opentelemetry.semconv.SemanticAttributes.NET_PROTOCOL_VERSION;
import static io.opentelemetry.semconv.SemanticAttributes.NET_SOCK_PEER_ADDR;
import static io.opentelemetry.semconv.SemanticAttributes.NET_SOCK_PEER_NAME;
import static io.opentelemetry.semconv.SemanticAttributes.NET_SOCK_PEER_PORT;
import static io.opentelemetry.semconv.SemanticAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.SemanticAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.SemanticAttributes.SERVER_SOCKET_ADDRESS;
import static io.opentelemetry.semconv.SemanticAttributes.SERVER_SOCKET_DOMAIN;
import static io.opentelemetry.semconv.SemanticAttributes.SERVER_SOCKET_PORT;
import static io.opentelemetry.semconv.SemanticAttributes.URL_FULL;
import static io.opentelemetry.semconv.SemanticAttributes.URL_PATH;
import static io.opentelemetry.semconv.SemanticAttributes.URL_QUERY;
import static io.opentelemetry.semconv.SemanticAttributes.URL_SCHEME;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.api.internal.SemconvStability;
import io.opentelemetry.sdk.trace.data.SpanData;

public class AttributeKeysStability {
    @SuppressWarnings("unchecked")
    public static <T> T get(SpanData spanData, AttributeKey<T> key) {
        if (SemconvStability.emitOldHttpSemconv()) {
            if (SERVER_ADDRESS.equals(key)) {
                key = (AttributeKey<T>) NET_HOST_NAME;
            } else if (SERVER_PORT.equals(key)) {
                key = (AttributeKey<T>) NET_HOST_PORT;
            } else if (URL_SCHEME.equals(key)) {
                key = (AttributeKey<T>) HTTP_SCHEME;
            } else if (URL_FULL.equals(key)) {
                key = (AttributeKey<T>) HTTP_URL;
            } else if (URL_PATH.equals(key)) {
                key = (AttributeKey<T>) HTTP_TARGET;
            } else if (HTTP_REQUEST_METHOD.equals(key)) {
                key = (AttributeKey<T>) HTTP_METHOD;
            } else if (HTTP_REQUEST_BODY_SIZE.equals(key)) {
                key = (AttributeKey<T>) HTTP_REQUEST_CONTENT_LENGTH;
            } else if (HTTP_RESPONSE_STATUS_CODE.equals(key)) {
                key = (AttributeKey<T>) HTTP_STATUS_CODE;
            } else if (HTTP_RESPONSE_BODY_SIZE.equals(key)) {
                key = (AttributeKey<T>) HTTP_RESPONSE_CONTENT_LENGTH;
            } else if (CLIENT_ADDRESS.equals(key)) {
                key = (AttributeKey<T>) HTTP_CLIENT_IP;
            } else if (SERVER_SOCKET_ADDRESS.equals(key) || CLIENT_SOCKET_ADDRESS.equals(key)) {
                key = (AttributeKey<T>) NET_SOCK_PEER_ADDR;
            } else if (SERVER_SOCKET_PORT.equals(key) || CLIENT_SOCKET_PORT.equals(key)) {
                key = (AttributeKey<T>) NET_SOCK_PEER_PORT;
            } else if (NETWORK_PROTOCOL_NAME.equals(key)) {
                key = (AttributeKey<T>) NET_PROTOCOL_NAME;
            } else if (NETWORK_PROTOCOL_VERSION.equals(key)) {
                key = (AttributeKey<T>) NET_PROTOCOL_VERSION;
            } else if (NET_SOCK_PEER_NAME.equals(key)) {
                key = (AttributeKey<T>) SERVER_SOCKET_DOMAIN;
            }
        } else {
            if (URL_PATH.equals(key)) {
                String path = spanData.getAttributes().get(URL_PATH);
                String query = spanData.getAttributes().get(URL_QUERY);
                if (path == null && query == null) {
                    return null;
                }
                String target = (path == null ? "" : path) + (query == null || query.isEmpty() ? "" : "?" + query);
                return (T) target;
            }
        }
        return spanData.getAttributes().get((AttributeKey<? extends T>) key);
    }
}
