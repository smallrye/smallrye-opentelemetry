package io.smallrye.opentelemetry.tck;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import io.smallrye.opentelemetry.jaxrs2.client.ClientTracingFeature;
import io.smallrye.opentelemetry.jaxrs2.server.ServerTracingDynamicFeature;
import io.smallrye.opentelemetry.jaxrs2.server.SpanFinishingFilter;

/**
 * @author Pavol Loffay, Felix Wong
 */
@WebListener
public class ServletContextTracingInstaller implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        servletContext.setInitParameter("resteasy.providers",
                ServerTracingDynamicFeature.class.getName() + "," + ClientTracingFeature.class.getName());

        // Span finishing filter
        Dynamic filterRegistration = servletContext.addFilter("spanFinishingFilter", new SpanFinishingFilter());
        filterRegistration.setAsyncSupported(true);
        filterRegistration.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class),
                true, "*");
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }
}
