package io.quarkus.opentelemetry.runtime.config;

import io.smallrye.config.ConfigMapping;

public interface OtelConnectionConfig {
    String endpoint();

     String certificate();

     OtelClientConfig client();

     String headers();

     String compression();

     String timeout();

     String protocol();

//    @ConfigGroup()
    public interface OtelClientConfig {

         String key();

         String certificate();
    }
}
