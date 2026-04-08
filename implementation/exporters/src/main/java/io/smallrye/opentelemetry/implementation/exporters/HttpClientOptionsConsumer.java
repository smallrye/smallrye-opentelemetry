package io.smallrye.opentelemetry.implementation.exporters;

import java.net.URI;
import java.util.function.Consumer;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.TrustOptions;

class HttpClientOptionsConsumer implements Consumer<HttpClientOptions> {
    private final ExporterConfiguration config;

    public HttpClientOptionsConsumer(ExporterConfiguration config) {
        this.config = config;
    }

    @Override
    public void accept(HttpClientOptions options) {
        configureTLS(options);

        if (config.isProxyEnabled()) {
            configureProxyOptions(options);
        }
    }

    private void configureTLS(HttpClientOptions options) {
        KeyCertOptions keyCertOptions = config.getKeyCertOptions();
        if (keyCertOptions != null) {
            options.setKeyCertOptions(keyCertOptions);
        }

        TrustOptions trustOptions = config.getTrustOptions();
        if (trustOptions != null) {
            options.setTrustOptions(trustOptions);
        }

        URI endpoint = config.getEndpoint();
        if ("https".equalsIgnoreCase(endpoint.getScheme())) {
            options.setSsl(true);
            options.setUseAlpn(true);
        }

        if (config.isTrustAll()) {
            options.setTrustAll(true);
            options.setVerifyHost(false);
        }
    }

    private void configureProxyOptions(HttpClientOptions options) {
        config.getProxyHost().ifPresentOrElse(
                host -> {
                    ProxyOptions proxyOptions = new ProxyOptions().setHost(host);
                    config.getProxyPort().ifPresent(proxyOptions::setPort);
                    config.getProxyUsername().ifPresent(proxyOptions::setUsername);
                    config.getProxyPassword().ifPresent(proxyOptions::setPassword);
                    options.setProxyOptions(proxyOptions);
                },
                () -> configureProxyOptionsFromJDKSysProps(options));
    }

    private void configureProxyOptionsFromJDKSysProps(HttpClientOptions options) {
        String proxyHost = options.isSsl()
                ? System.getProperty("https.proxyHost", "none")
                : System.getProperty("http.proxyHost", "none");
        String proxyPortAsString = options.isSsl()
                ? System.getProperty("https.proxyPort", "443")
                : System.getProperty("http.proxyPort", "80");
        int proxyPort = Integer.parseInt(proxyPortAsString);

        if (!"none".equals(proxyHost)) {
            ProxyOptions proxyOptions = new ProxyOptions().setHost(proxyHost).setPort(proxyPort);
            String proxyUser = options.isSsl()
                    ? System.getProperty("https.proxyUser")
                    : System.getProperty("http.proxyUser");
            if (proxyUser != null && !proxyUser.isBlank()) {
                proxyOptions.setUsername(proxyUser);
            }
            String proxyPassword = options.isSsl()
                    ? System.getProperty("https.proxyPassword")
                    : System.getProperty("http.proxyPassword");
            if (proxyPassword != null && !proxyPassword.isBlank()) {
                proxyOptions.setPassword(proxyPassword);
            }
            options.setProxyOptions(proxyOptions);
        }
    }
}
