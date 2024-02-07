package io.smallrye.opentelemetry.test;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import io.smallrye.opentelemetry.test.metrics.InMemoryMetricExporter;
import io.smallrye.opentelemetry.test.trace.InMemorySpanExporter;

public class DeploymentProcessor implements ApplicationArchiveProcessor {
    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        if (archive instanceof WebArchive) {
            WebArchive war = (WebArchive) archive;
            war.addAsServiceProvider(ConfigSource.class, TestConfigSource.class);
            war.addClass(HttpServerAttributesFilter.class);
            war.addClass(InMemorySpanExporter.class);
            war.addClass(InMemoryMetricExporter.class);
        }
    }
}
