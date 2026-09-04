package com.issueflow.logging;

import com.issueflow.constants.LoggingConstants;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;

public final class OperationalLog {

    private OperationalLog() {
    }

    public static Fields event(String event) {
        return new Fields(event);
    }

    public static String format(String event, Map<String, Object> fields) {
        StringBuilder message = new StringBuilder(LoggingConstants.EVENT).append('=').append(event);
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            message.append(' ').append(entry.getKey()).append('=').append(entry.getValue());
        }
        return message.toString();
    }

    public static final class Fields {

        private final String event;
        private final Map<String, Object> fields = new LinkedHashMap<>();

        private Fields(String event) {
            this.event = event;
        }

        public Fields put(String key, Object value) {
            if (value != null) {
                fields.put(key, value);
            }
            return this;
        }

        public void info(Logger logger) {
            emit(logger, Level.INFO, null);
        }

        public void warn(Logger logger) {
            emit(logger, Level.WARN, null);
        }

        public void debug(Logger logger) {
            emit(logger, Level.DEBUG, null);
        }

        public void error(Logger logger, Throwable throwable) {
            emit(logger, Level.ERROR, throwable);
        }

        private void emit(Logger logger, Level level, Throwable throwable) {
            Map<String, String> previous = new LinkedHashMap<>();
            putMdc(LoggingConstants.EVENT, event, previous);
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                putMdc(entry.getKey(), String.valueOf(entry.getValue()), previous);
            }
            String message = format(event, fields);
            try {
                switch (level) {
                    case DEBUG -> logger.debug(message);
                    case WARN -> logger.warn(message);
                    case ERROR -> {
                        if (throwable == null) {
                            logger.error(message);
                        } else {
                            logger.error(message, throwable);
                        }
                    }
                    case INFO -> logger.info(message);
                }
            } finally {
                restoreMdc(previous);
            }
        }

        private static void putMdc(String key, String value, Map<String, String> previous) {
            previous.put(key, MDC.get(key));
            MDC.put(key, value);
        }

        private static void restoreMdc(Map<String, String> previous) {
            for (Map.Entry<String, String> entry : previous.entrySet()) {
                if (entry.getValue() == null) {
                    MDC.remove(entry.getKey());
                } else {
                    MDC.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private enum Level {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
}
