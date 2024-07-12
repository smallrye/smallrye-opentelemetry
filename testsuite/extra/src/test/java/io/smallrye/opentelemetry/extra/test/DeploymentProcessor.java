package io.smallrye.opentelemetry.extra.test;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class DeploymentProcessor implements ApplicationArchiveProcessor {
    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        if (archive instanceof WebArchive) {
            WebArchive war = (WebArchive) archive;
            war.addAsServiceProvider(ConfigSource.class, TestConfigSource.class);
            war.addClass(HttpServerAttributesFilter.class);
        }
    }
}
