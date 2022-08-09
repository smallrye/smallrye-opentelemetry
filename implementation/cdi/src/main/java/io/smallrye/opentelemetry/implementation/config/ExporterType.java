package io.smallrye.opentelemetry.implementation.config;

public enum ExporterType {
    OTLP(Constants.OTLP_VALUE),
    //        HTTP(Constants.HTTP_VALUE), // TODO not supported yet
    JAEGER(Constants.JAEGER),
    NONE(Constants.NONE_VALUE);

    private String value;

    ExporterType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static class Constants {
        public static final String OTLP_VALUE = "otlp";
        //            public static final String HTTP_VALUE = "http";
        public static final String NONE_VALUE = "none";
        public static final String JAEGER = "jaeger";
    }
}
