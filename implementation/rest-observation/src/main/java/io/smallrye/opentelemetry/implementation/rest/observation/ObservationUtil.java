package io.smallrye.opentelemetry.implementation.rest.observation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.docs.KeyName;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;

public class ObservationUtil {
    public static final String UNKNOWN = "UNKNOWN";
    public static final KeyValue STATUS_UNKNOWN = KeyValue
            .of(FilterDocumentation.LowCardinalityValues.HTTP_RESPONSE_STATUS_CODE, UNKNOWN);
    public static final KeyValue USER_AGENT_UNKNOWN = KeyValue
            .of(FilterDocumentation.HighCardinalityValues.USER_AGENT_ORIGINAL, UNKNOWN);
    public static final KeyValue QUERY_UNKNOWN = KeyValue
            .of(FilterDocumentation.HighCardinalityValues.URL_QUERY, UNKNOWN);

    private ObservationUtil() {
        //no content
    }

    // FIXME this is a hack because KeyValue does not support non-string values
    public static KeyValue collectAttribute(final AttributesBuilder attributesBuilder,
            final KeyName keyName,
            final Object value) {
        if (value == null) {
            return keyName.withValue(UNKNOWN);
        } else if (value instanceof String) {
            attributesBuilder.put(AttributeKey.stringKey(keyName.asString()), (String) value);
        } else if (value instanceof Long) {
            attributesBuilder.put(AttributeKey.longKey(keyName.asString()), (Long) value);
        } else if (value instanceof Integer) {
            attributesBuilder.put(AttributeKey.longKey(keyName.asString()), Long.valueOf((Integer) value));
        } else {
            throw new IllegalArgumentException("Unsupported value type: " + value.getClass());
        }
        return keyName.withValue(String.valueOf(value));
    }
}
