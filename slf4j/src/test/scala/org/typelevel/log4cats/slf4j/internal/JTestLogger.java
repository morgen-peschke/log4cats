/*
 * Copyright 2018 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.typelevel.log4cats.slf4j.internal;

import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.typelevel.log4cats.extras.LogLevel;
import scala.Option;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class JTestLogger implements Logger {
    // Java -> Scala compat helpers

    private static final scala.Option<Throwable> none = scala.Option$.MODULE$.empty();
    private static scala.Option<Throwable> some(Throwable t) { return scala.Option$.MODULE$.apply(t); }
    private static final LogLevel.Trace$ Trace = LogLevel.Trace$.MODULE$;
    private static final LogLevel.Debug$ Debug = LogLevel.Debug$.MODULE$;
    private static final LogLevel.Info$ Info = LogLevel.Info$.MODULE$;
    private static final LogLevel.Warn$ Warn = LogLevel.Warn$.MODULE$;
    private static final LogLevel.Error$ Error = LogLevel.Error$.MODULE$;

    private Map<String, String> captureContext () {
        java.util.Map<String, String> mdc = MDC.getCopyOfContextMap();
        if (mdc == null) {
            return new HashMap<>();
        }
        return MDC.getCopyOfContextMap();
    }

    public static class TestLogMessage {
        public final LogLevel logLevel;
        public final java.util.Map<String, String> context;
        public final Option<Throwable> throwableOpt;
        public final Supplier<String> message;

        public TestLogMessage(LogLevel logLevel,
                              java.util.Map<String, String> context,
                              Option<Throwable> throwableOpt,
                              Supplier<String> message) {
            this.logLevel = logLevel;
            this.context = context;
            this.throwableOpt = throwableOpt;
            this.message = message;
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append("TestLogMessage(")
                    .append("logLevel=").append(logLevel)
                    .append(", ")
                    .append("context=").append(context)
                    .append(", ")
                    .append("throwableOpt=").append(throwableOpt)
                    .append(", ")
                    .append("message=").append(message.get())
                    .append(')')
                    .toString();
        }

        static TestLogMessage of(LogLevel logLevel,
                                 java.util.Map<String, String> context,
                                 Throwable throwable,
                                 Supplier<String> message) {
            return new TestLogMessage(logLevel, context, some(throwable), message);
        }

        static TestLogMessage of(LogLevel logLevel,
                                 java.util.Map<String, String> context,
                                 Supplier<String> message) {
            return new TestLogMessage(logLevel, context, none, message);
        }
    }

    private final String loggerName;
    private final boolean traceEnabled;
    private final boolean debugEnabled;
    private final boolean infoEnabled;
    private final boolean warnEnabled;
    private final boolean errorEnabled;
    private final AtomicReference<List<TestLogMessage>> loggedMessages;


    public JTestLogger(String loggerName,
                       boolean traceEnabled,
                       boolean debugEnabled,
                       boolean infoEnabled,
                       boolean warnEnabled,
                       boolean errorEnabled) {
        this.loggerName = loggerName;
        this.traceEnabled = traceEnabled;
        this.debugEnabled = debugEnabled;
        this.infoEnabled = infoEnabled;
        this.warnEnabled = warnEnabled;
        this.errorEnabled = errorEnabled;
        loggedMessages = new AtomicReference<>(new ArrayList<TestLogMessage>());
    }

    private void save(Function<Map<String, String>, TestLogMessage> mkLogMessage) {
        loggedMessages.updateAndGet(ll -> {
            ll.add(mkLogMessage.apply(captureContext()));
            return ll;
        });
    }

    public List<TestLogMessage> logs() { return loggedMessages.get(); }
    public void reset() { loggedMessages.set(new ArrayList<>()); }

    @Override public String getName() { return loggerName;}

    @Override public boolean isTraceEnabled() { return traceEnabled; }
    @Override public boolean isDebugEnabled() { return debugEnabled; }
    @Override public boolean isInfoEnabled() { return infoEnabled; }
    @Override public boolean isWarnEnabled() { return warnEnabled; }
    @Override public boolean isErrorEnabled() { return errorEnabled; }

    // We don't use them, so we're going to ignore Markers
    @Override public boolean isTraceEnabled(Marker marker) { return traceEnabled; }
    @Override public boolean isDebugEnabled(Marker marker) { return debugEnabled; }
    @Override public boolean isInfoEnabled(Marker marker) { return infoEnabled; }
    @Override public boolean isWarnEnabled(Marker marker) { return warnEnabled; }
    @Override public boolean isErrorEnabled(Marker marker) { return errorEnabled; }

    @Override public void trace(String msg) { save(ctx -> TestLogMessage.of(Trace, ctx, () -> msg)); }
    @Override public void trace(String msg, Throwable t) { save(ctx -> TestLogMessage.of(Trace, ctx, t, () -> msg)); }

    @Override public void debug(String msg) { save(ctx -> TestLogMessage.of(Debug, ctx, () -> msg)); }
    @Override public void debug(String msg, Throwable t) { save(ctx -> TestLogMessage.of(Debug, ctx, t, () -> msg)); }

    @Override public void info(String msg) { save(ctx -> TestLogMessage.of(Info, ctx, () -> msg)); }
    @Override public void info(String msg, Throwable t) { save(ctx -> TestLogMessage.of(Info, ctx, t, () -> msg)); }

    @Override public void warn(String msg) { save(ctx -> TestLogMessage.of(Warn, ctx, () -> msg)); }
    @Override public void warn(String msg, Throwable t) { save(ctx -> TestLogMessage.of(Warn, ctx, t, () -> msg)); }

    @Override public void error(String msg) { save(ctx -> TestLogMessage.of(Error, ctx, () -> msg)); }
    @Override public void error(String msg, Throwable t) { save(ctx -> TestLogMessage.of(Error, ctx, t, () -> msg)); }

    // We shouldn't need these for our tests, so we're treating these variants as if they were the standard method

    @Override public void trace(String format, Object arg)                              { trace(format); }
    @Override public void trace(String format, Object arg1, Object arg2)                { trace(format); }
    @Override public void trace(String format, Object... arguments)                     { trace(format); }
    @Override public void trace(Marker marker, String msg)                              { trace(msg);    }
    @Override public void trace(Marker marker, String format, Object arg)               { trace(format); }
    @Override public void trace(Marker marker, String format, Object arg1, Object arg2) { trace(format); }
    @Override public void trace(Marker marker, String format, Object... argArray)       { trace(format); }
    @Override public void trace(Marker marker, String msg, Throwable t)                 { trace(msg, t); }

    @Override public void debug(String format, Object arg)                              { debug(format); }
    @Override public void debug(String format, Object arg1, Object arg2)                { debug(format); }
    @Override public void debug(String format, Object... arguments)                     { debug(format); }
    @Override public void debug(Marker marker, String msg)                              { debug(msg);    }
    @Override public void debug(Marker marker, String format, Object arg)               { debug(format); }
    @Override public void debug(Marker marker, String format, Object arg1, Object arg2) { debug(format); }
    @Override public void debug(Marker marker, String format, Object... arguments) { debug(format); }
    @Override public void debug(Marker marker, String msg, Throwable t) { debug(msg, t); }

    @Override public void info(String format, Object arg)                               { info(format); }
    @Override public void info(String format, Object arg1, Object arg2)                 { info(format); }
    @Override public void info(String format, Object... arguments)                      { info(format); }
    @Override public void info(Marker marker, String msg)                               { info(msg);    }
    @Override public void info(Marker marker, String format, Object arg)                { info(format); }
    @Override public void info(Marker marker, String format, Object arg1, Object arg2)  { info(format); }
    @Override public void info(Marker marker, String format, Object... arguments)       { info(format); }
    @Override public void info(Marker marker, String msg, Throwable t)                  { info(msg, t); }

    @Override public void warn(String format, Object arg)                               { warn(format); }
    @Override public void warn(String format, Object... arguments)                      { warn(format); }
    @Override public void warn(String format, Object arg1, Object arg2)                 { warn(format); }
    @Override public void warn(Marker marker, String msg)                               { warn(msg);    }
    @Override public void warn(Marker marker, String format, Object arg)                { warn(format); }
    @Override public void warn(Marker marker, String format, Object arg1, Object arg2)  { warn(format); }
    @Override public void warn(Marker marker, String format, Object... arguments)       { warn(format); }
    @Override public void warn(Marker marker, String msg, Throwable t)                  { warn(msg, t); }

    @Override public void error(String format, Object arg)                              { error(format); }
    @Override public void error(String format, Object arg1, Object arg2)                { error(format); }
    @Override public void error(String format, Object... arguments)                     { error(format); }
    @Override public void error(Marker marker, String msg)                              { error(msg);    }
    @Override public void error(Marker marker, String format, Object arg)               { error(format); }
    @Override public void error(Marker marker, String format, Object arg1, Object arg2) { error(format); }
    @Override public void error(Marker marker, String format, Object... arguments)      { error(format); }
    @Override public void error(Marker marker, String msg, Throwable t)                 { error(msg, t); }
}