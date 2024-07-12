package io.smallrye.opentelemetry.tck;

import java.io.PrintStream;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import io.smallrye.common.constraint.Assert;

public class RedirectOutHandler extends Handler {
    private final PrintStream oldOut;
    private final PrintStream newOut;

    public RedirectOutHandler() throws Exception {
        LogManager manager = LogManager.getLogManager();

        String className = this.getClass().getName();
        String pattern = Assert.checkNotNullParam(className + ".pattern", manager.getProperty(className + ".pattern"));

        this.oldOut = System.out;
        this.newOut = new PrintStream(pattern);
        System.setOut(newOut);
    }

    @Override
    public void publish(final LogRecord record) {

    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {
        newOut.close();
        System.setOut(oldOut);
    }
}
