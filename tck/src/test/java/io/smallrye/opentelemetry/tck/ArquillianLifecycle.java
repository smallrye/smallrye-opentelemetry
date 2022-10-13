package io.smallrye.opentelemetry.tck;

import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.container.spi.event.container.AfterDeploy;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.logs.GlobalLoggerProvider;
import io.restassured.RestAssured;

public class ArquillianLifecycle {
    public void beforeDeploy(@Observes BeforeDeploy event, TestClass testClass) {
        GlobalOpenTelemetry.resetForTest();
        GlobalLoggerProvider.resetForTest();
    }

    @Inject
    Instance<ProtocolMetaData> protocolMetadata;

    public void afterDeploy(@Observes AfterDeploy event, TestClass testClass) {
        HTTPContext httpContext = protocolMetadata.get().getContexts(HTTPContext.class).iterator().next();
        Servlet servlet = httpContext.getServlets().iterator().next();
        String baseUri = servlet.getBaseURI().toString();
        TestConfigSource.configuration.put("baseUri", baseUri);

        RestAssured.port = httpContext.getPort();
        RestAssured.basePath = servlet.getBaseURI().getPath();
    }
}
