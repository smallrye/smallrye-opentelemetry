package io.smallrye.opentelemetry.observation.test;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.data.SpanData;

public class AttributeKeysStability {
    public static <T> T get(SpanData spanData, AttributeKey<T> key) {
        return spanData.getAttributes().get((AttributeKey<? extends T>) key);
    }
}
