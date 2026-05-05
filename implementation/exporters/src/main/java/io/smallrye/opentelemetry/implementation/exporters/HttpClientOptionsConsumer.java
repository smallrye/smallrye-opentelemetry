package io.smallrye.opentelemetry.implementation.exporters;

import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_CERTIFICATE;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_CLIENT_KEY;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_SIGNAL_CERTIFICATE;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_SIGNAL_CLIENT_CERTIFICATE;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.OTEL_EXPORTER_OTLP_SIGNAL_CLIENT_KEY;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.SROTEL_PROXY_ENABLED;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.SROTEL_PROXY_HOST;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.SROTEL_PROXY_PASSWORD;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.SROTEL_PROXY_PORT;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.SROTEL_PROXY_USERNAME;
import static io.smallrye.opentelemetry.implementation.exporters.Constants.SROTEL_TLS_TRUST_ALL;
import static io.smallrye.opentelemetry.implementation.exporters.OtlpExporterUtil.getConfig;

import java.net.URI;
import java.util.function.Consumer;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.ProxyOptions;

class HttpClientOptionsConsumer implements Consumer<HttpClientOptions> {
    private final ConfigProperties config;
    private final URI baseUri;
    private final String signalType;

    public HttpClientOptionsConsumer(ConfigProperties config, URI baseUri, String signalType) {
        this.config = config;
        this.baseUri = baseUri;
        this.signalType = signalType;
    }

    @Override
    public void accept(HttpClientOptions options) {
        configureTLS(options);

        if (Boolean.parseBoolean(getConfig(config, "false", SROTEL_PROXY_ENABLED))) {
            configureProxyOptions(options);
        }
    }

    private void configureTLS(HttpClientOptions options) {
        configureKeyCertOptions(options);
        configureTrustOptions(options);

        if (OtlpExporterUtil.isHttps(baseUri)) {
            options.setSsl(true);
            options.setUseAlpn(true);
        }

        if (Boolean.parseBoolean(getConfig(config, "false", SROTEL_TLS_TRUST_ALL))) {
            options.setTrustAll(true);
            options.setVerifyHost(false);
        }
    }

    private void configureProxyOptions(HttpClientOptions options) {
        var proxyHost = getConfig(config, "", SROTEL_PROXY_HOST);
        if (!proxyHost.isBlank()) {
            ProxyOptions proxyOptions = new ProxyOptions()
                    .setHost(proxyHost);
            var proxyPort = getConfig(config, "", SROTEL_PROXY_PORT);
            var proxyUsername = getConfig(config, "", SROTEL_PROXY_USERNAME);
            var proxyPassword = getConfig(config, "", SROTEL_PROXY_PASSWORD);

            if (!proxyPort.isBlank()) {
                proxyOptions.setPort(Integer.parseInt(proxyPort));
            }
            if (!proxyUsername.isBlank()) {
                proxyOptions.setUsername(proxyUsername);
            }
            if (!proxyPassword.isBlank()) {
                proxyOptions.setPassword(proxyPassword);
            }
            options.setProxyOptions(proxyOptions);
        } else {
            configureProxyOptionsFromJDKSysProps(options);
        }
    }

    private void configureProxyOptionsFromJDKSysProps(HttpClientOptions options) {
        var proxyHost = options.isSsl()
                ? System.getProperty("https.proxyHost", "none")
                : System.getProperty("http.proxyHost", "none");
        var proxyPortAsString = options.isSsl()
                ? System.getProperty("https.proxyPort", "443")
                : System.getProperty("http.proxyPort", "80");
        var proxyPort = Integer.parseInt(proxyPortAsString);

        if (!"none".equals(proxyHost)) {
            ProxyOptions proxyOptions = new ProxyOptions().setHost(proxyHost).setPort(proxyPort);
            var proxyUser = options.isSsl()
                    ? System.getProperty("https.proxyUser")
                    : System.getProperty("http.proxyUser");
            if (proxyUser != null && !proxyUser.isBlank()) {
                proxyOptions.setUsername(proxyUser);
            }
            var proxyPassword = options.isSsl()
                    ? System.getProperty("https.proxyPassword")
                    : System.getProperty("http.proxyPassword");
            if (proxyPassword != null && !proxyPassword.isBlank()) {
                proxyOptions.setPassword(proxyPassword);
            }
            options.setProxyOptions(proxyOptions);
        }
    }

    private void configureKeyCertOptions(HttpClientOptions options) {
        var pemKeyCertOptions = new PemKeyCertOptions();

        var certificate = getConfig(config, "",
                String.format(OTEL_EXPORTER_OTLP_SIGNAL_CLIENT_CERTIFICATE, signalType),
                OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE);
        var key = getConfig(config, "",
                String.format(OTEL_EXPORTER_OTLP_SIGNAL_CLIENT_KEY, signalType), OTEL_EXPORTER_OTLP_CLIENT_KEY);

        if (!certificate.isEmpty()) {
            pemKeyCertOptions.addCertPath(certificate);
        }

        if (!key.isEmpty()) {
            pemKeyCertOptions.addKeyPath(key);
        }
        options.setKeyCertOptions(pemKeyCertOptions);
    }

    private void configureTrustOptions(HttpClientOptions options) {
        var certificate = getConfig(config, "",
                String.format(OTEL_EXPORTER_OTLP_SIGNAL_CERTIFICATE, signalType), OTEL_EXPORTER_OTLP_CERTIFICATE);

        if (!certificate.isEmpty()) {
            var pemTrustOptions = new PemTrustOptions()
                    .addCertPath(certificate);
            options.setPemTrustOptions(pemTrustOptions);
        }
    }
}
