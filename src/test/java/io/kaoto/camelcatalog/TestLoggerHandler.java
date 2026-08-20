package io.kaoto.camelcatalog;

import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class TestLoggerHandler extends Handler {
    List<LogRecord> records;

    public TestLoggerHandler() {
        this.records = new java.util.ArrayList<>();
    }

    @Override
    public void publish(LogRecord logRecord) {
        records.add(logRecord);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
        records.clear();
    }

    public List<LogRecord> getRecords() {
        return records;
    }
}
