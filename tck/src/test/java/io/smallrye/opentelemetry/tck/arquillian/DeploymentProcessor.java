package io.smallrye.opentelemetry.tck.arquillian;

import java.io.File;

import javax.servlet.ServletContainerInitializer;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

/**
 * @author Pavol Loffay
 */
public class DeploymentProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        if (archive instanceof WebArchive) {
            JavaArchive extensionsJar = ShrinkWrap.create(JavaArchive.class, "extension.jar");
            extensionsJar.addPackages(true, "io.opentelemetry");

            // TODO add once CDI instrumentation is supported
            // install CDI extensions
            //            extensionsJar.addClasses(OpenTracingCDIExtension.class);

            WebArchive war = WebArchive.class.cast(archive);
            war.addAsLibraries(extensionsJar);

            // Workaround for RESTEASY-1922
            war.addClass(FixedResteasyServletInitializer.class);
            war.addAsServiceProvider(ServletContainerInitializer.class, FixedResteasyServletInitializer.class);

            String[] deps = {
                    "io.opentelemetry:opentelemetry-sdk",
                    "io.opentelemetry:opentelemetry-proto",
                    "io.grpc:grpc-netty",
                    "io.smallrye:smallrye-opentelemetry-jaxrs2",
                    // TODO add once MP-RestClient instrumentation is supported
                    //  "org.jboss.resteasy:resteasy-client-microprofile",
                    "org.jboss.resteasy:resteasy-client",
                    "org.jboss.resteasy:resteasy-json-binding-provider",
                    "org.jboss.resteasy:resteasy-cdi",
                    "org.jboss.weld.servlet:weld-servlet-core",
            };
            File[] dependencies = Maven.resolver()
                    .loadPomFromFile(new File("pom.xml"))
                    .resolve(deps)
                    .withTransitivity()
                    .asFile();
            war.addAsLibraries(dependencies);
            System.out.println(war.toString(true));
        }
    }
}
