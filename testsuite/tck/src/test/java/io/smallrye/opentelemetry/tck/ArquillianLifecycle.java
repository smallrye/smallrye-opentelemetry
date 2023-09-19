package io.smallrye.opentelemetry.tck;

import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;

import io.opentelemetry.api.GlobalOpenTelemetry;

public class ArquillianLifecycle {
    public void beforeDeploy(@Observes BeforeDeploy event, TestClass testClass) {
        GlobalOpenTelemetry.resetForTest();
    }
}
