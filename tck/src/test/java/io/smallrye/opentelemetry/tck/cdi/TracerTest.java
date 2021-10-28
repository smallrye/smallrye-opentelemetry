package io.smallrye.opentelemetry.tck.cdi;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.opentelemetry.api.trace.Tracer;

@ExtendWith(ArquillianExtension.class)
public class TracerTest {
    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap
                .create(WebArchive.class)
                .addClass(TracerBean.class);
    }

    @Inject
    TracerBean tracerBean;

    @Test
    void tracer() {
        assertNotNull(tracerBean.getTracer());
    }

    @ApplicationScoped
    public static class TracerBean {
        @Inject
        Tracer tracer;

        public Tracer getTracer() {
            return tracer;
        }
    }
}
