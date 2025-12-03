package io.smallrye.opentelemetry.extra.test.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.smallrye.opentelemetry.api.OpenTelemetryConfig;
import io.smallrye.opentelemetry.api.OpenTelemetryLogHandler;
import io.smallrye.opentelemetry.implementation.cdi.OpenTelemetryProducer;
import io.smallrye.opentelemetry.test.InMemoryExporter;

@ExtendWith(ArquillianExtension.class)
public class LogsTest {
    @Inject
    InMemoryExporter logExporter;

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    OpenTelemetryConfig config;

    @BeforeAll
    public static void setup() {
        var rootLogger = LogManager.getLogManager().getLogger("");

        Arrays.stream(rootLogger.getHandlers()).filter(h -> h instanceof OpenTelemetryLogHandler)
                .findFirst()
                .ifPresent(rootLogger::removeHandler);
    }

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsResource(new StringAsset("otel.logs.exporter=in-memory"), "META-INF/microprofile-config.properties");
    }

    @BeforeEach
    void setUp() {
        logExporter.reset();
    }

    @Test
    void testLogMessageFormatting() throws InterruptedException {
        Logger logger = Logger.getLogger(getClass().getName());
        logger.log(Level.INFO, "Test %s", "message");

        logExporter.assertFinishedLogRecordItems(items -> assertTrue(items.stream().anyMatch(r -> {
            var value = r.getBodyValue();
            return value != null && value.asString().contains("Test message");
        }), "Log message not found"),
                Duration.ofSeconds(60));
    }

    @Test
    void testDuplicatedMessages() throws InterruptedException {
        // Create another instance to simulate multiple deployments in the same JVM
        new OpenTelemetryProducer().getOpenTelemetry(config);

        final String testMessage = "Should not be duplicated";
        Logger.getLogger(getClass().getName()).log(Level.INFO, testMessage);

        logExporter.assertFinishedLogRecordItems(items -> assertEquals(1, items.stream().map(LogRecordData::getBodyValue)
                .filter(Objects::nonNull)
                .map(Value::asString)
                .filter(s -> s.equals(testMessage))
                .count(), "Duplicated log message found"),
                Duration.ofSeconds(5));
    }
}
