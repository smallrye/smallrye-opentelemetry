package io.smallrye.opentelemetry.api;

import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_MESSAGE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_TYPE;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_FUNCTION_NAME;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

public class OpenTelemetryLogHandler extends Handler {
    private final String loggerClassName;
    private static final AttributeKey<String> NAMESPACE_ATTRIBUTE_KEY = AttributeKey.stringKey("log.logger.namespace");
    // See: https://github.com/open-telemetry/semantic-conventions/issues/1550
    public static final AttributeKey<String> BRIDGE_NAME = AttributeKey.stringKey("bridge.name");

    private final OpenTelemetry openTelemetry;

    public OpenTelemetryLogHandler(final OpenTelemetry openTelemetry, final Logger logger) {
        this.openTelemetry = openTelemetry;
        this.loggerClassName = logger.getClass().getName();
    }

    public static void install(final OpenTelemetry openTelemetry) {
        LogManager.getLogManager().getLogger("").addHandler(new OpenTelemetryLogHandler(openTelemetry,
                openTelemetry.getLogsBridge().loggerBuilder(OpenTelemetryConfig.INSTRUMENTATION_NAME).build()));
    }

    @Override
    public void publish(final LogRecord record) {
        if (openTelemetry != null) { // Might happen at shutdown
            final LogRecordBuilder logRecordBuilder = openTelemetry.getLogsBridge()
                    .loggerBuilder(OpenTelemetryConfig.INSTRUMENTATION_NAME)
                    .build().logRecordBuilder()
                    .setTimestamp(Instant.now())
                    .setObservedTimestamp(record.getInstant());

            if (record.getLevel() != null) {
                logRecordBuilder
                        .setSeverity(toSeverity(record.getLevel()))
                        .setSeverityText(record.getLevel().getName());
            }

            if (record.getMessage() != null) {
                final Formatter formatter = getFormatter();
                logRecordBuilder.setBody(formatter != null ? formatter.format(record) : getFormattedMessage(record));
            }

            final AttributesBuilder attributes = Attributes.builder()
                    .put(CODE_FUNCTION_NAME, record.getSourceClassName() + "." + record.getSourceMethodName())
                    .put(NAMESPACE_ATTRIBUTE_KEY, loggerClassName)
                    .put(BRIDGE_NAME, record.getLoggerName());

            if (record.getThrown() != null) {
                try (StringWriter sw = new StringWriter(1024); PrintWriter pw = new PrintWriter(sw)) {
                    record.getThrown().printStackTrace(pw);
                    sw.flush();
                    attributes.put(EXCEPTION_STACKTRACE, sw.toString());
                } catch (Throwable t) {
                    attributes.put(EXCEPTION_STACKTRACE, "Unable to get the stacktrace of the exception");
                }
                attributes
                        .put(EXCEPTION_TYPE, record.getThrown().getClass().getName())
                        .put(EXCEPTION_MESSAGE, record.getThrown().getMessage());
            }

            logRecordBuilder.setAllAttributes(attributes.build());
            logRecordBuilder.emit();
        }
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }

    private Severity toSeverity(final Level level) {
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

    private String getFormattedMessage(LogRecord record) {
        final ResourceBundle bundle = record.getResourceBundle();
        String msg = record.getMessage();
        if (msg == null) {
            return null;
        }
        if (bundle != null) {
            try {
                msg = bundle.getString(msg);
            } catch (MissingResourceException ex) {
                // ignore
            }
        }
        final Object[] parameters = record.getParameters();
        if (parameters == null || parameters.length == 0) {
            return msg;
        }
        return msg.indexOf('{') >= 0 ? MessageFormat.format(msg, parameters) : String.format(msg, parameters);
    }
}
