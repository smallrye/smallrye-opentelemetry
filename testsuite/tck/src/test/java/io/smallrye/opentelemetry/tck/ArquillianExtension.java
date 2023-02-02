package io.smallrye.opentelemetry.tck;

import org.jboss.arquillian.core.spi.LoadableExtension;

public class ArquillianExtension implements LoadableExtension {
    @Override
    public void register(ExtensionBuilder extensionBuilder) {
        extensionBuilder.observer(ArquillianLifecycle.class);
    }
}
