package io.smallrye.opentelemetry.api;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

public class OpenTelemetryHandler extends Handler {
    private final Logger logger;

    public OpenTelemetryHandler(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void publish(final LogRecord record) {
        logger.logRecordBuilder()
                .setTimestamp(record.getInstant())
                .setSeverity(toSeverity(record.getLevel()))
                .setSeverityText(record.getLevel().getName())
                .setBody(record.getMessage())
                .emit();
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }

    private static Severity toSeverity(final Level level) {
        if (Level.SEVERE.equals(level)) {
            return Severity.ERROR;
        }
        if (Level.WARNING.equals(level)) {
            return Severity.WARN;
        }
        if (Level.INFO.equals(level) || Level.CONFIG.equals(level)) {
            return Severity.INFO;
        }
        if (Level.FINE.equals(level)) {
            return Severity.DEBUG;
        }
        if (Level.FINER.equals(level) || Level.FINEST.equals(level) || Level.ALL.equals(level)) {
            return Severity.TRACE;
        }
        return Severity.UNDEFINED_SEVERITY_NUMBER;
    }

    public static void install(final OpenTelemetry openTelemetry) {
        Logger logger = openTelemetry.getLogsBridge()
                .loggerBuilder(OpenTelemetryConfig.INSTRUMENTATION_NAME)
                .setInstrumentationVersion(OpenTelemetryConfig.INSTRUMENTATION_VERSION)
                .build();
        LogManager.getLogManager().getLogger("").addHandler(new OpenTelemetryHandler(logger));
    }
}
