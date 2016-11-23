/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.common.core.log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;

/**
 * Scope logger helper
 *
 * This is a logger helper, it will allow entry-exit analysis to be much easier.
 *
 * The events are saved in a JSON-like message in the phase of the event. It is
 * an event type but with extra information associate to it. Typical types can
 * be the following.
 * <ul>
 * <li>Durations
 * <ul>
 * <li><strong>B</strong>, Begin</li>
 * <li><strong>E</strong>, End</li>
 * <li><strong>X</strong>, Complete, this is an event with a duration field</li>
 * <li><strong>I</strong>, Instant / Info</li>
 * </ul>
 * </li>
 * <li>Asynchronous nested messages
 * <ul>
 * <li><strong>b</strong>, nested begin</li>
 * <li><strong>n</strong>, nested info</li>
 * <li><strong>e</strong>, nested end</li>
 * </ul>
 * </li>
 * <li>Object tracking
 * <ul>
 * <li><strong>N</Strong>, Object created</li>
 * <li><strong>D</Strong>, Object destroyed</li>
 * </ul>
 * </li>
 * <li>Mark Events - events that will generate markers
 * <ul>
 * <li><strong>R</strong>, Marker event</li>
 * </ul>
 * </ul>
 * <p>
 * To use <strong>durations</strong>, see {@link ScopeLog}.
 * <p>
 * To use <strong>Asynchronous nested messages</strong>, see
 * {@link #traceAsyncStart(Logger, Level, String, String, int, Object...)}, and
 * {@link #traceAsyncEnd(Logger, Level, String, String, int, Object...)}
 * <p>
 * To use <strong>Object tracking</strong>, see
 * {@link #traceObjectCreation(Logger, Level, Object)} and
 * {@link #traceObjectDestruction(Logger, Level, Object)}
 *
 * The design philosophy of this class is very heavily inspired by the trace
 * event format of Google. The full specification is available <a
 * href=https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU/edit?pli=1#>here</a>.
 * <p>
 *
 * The main goals are clarity of output and simplicity for the developer.
 * Performance is a nice to have, but is not the main concern of this helper. A
 * minor performance impact is to be expected.
 *
 * @author Matthew Khouzam
 * @since 2.2
 * @noinstantiate This class is not intended to be instantiated by clients. It
 *                is a helper class.
 */
public final class TraceCompassLogUtils {

    /*
     * Fields
     */
    private static final String ARGS = "args"; //$NON-NLS-1$
    private static final String NAME = "name"; //$NON-NLS-1$
    private static final String CATEGORY = "cat"; //$NON-NLS-1$
    private static final String ID = "id"; //$NON-NLS-1$
    private static final String TID = "tid"; //$NON-NLS-1$
    private static final String TIMESTAMP = "ts"; //$NON-NLS-1$
    private static final String PHASE = "ph"; //$NON-NLS-1$

    private static final String ARGS_ERROR_MESSAGE = "Data should be in the form of key, value, key1, value1, ... TraceCompassScopeLog was supplied "; //$NON-NLS-1$

    private TraceCompassLogUtils() {
        // do nothing
    }

    /**
     * Scope Logger helper,
     *
     * This is a logger helper, it will allow entry-exit analysis to be much
     * easier.
     *
     * Usage
     *
     * <pre>
     * {@code usage of ScopeLog}
     *  try (ScopeLog linksLogger = new ScopeLog(LOGGER, Level.CONFIG, "Perform Query")) { //$NON-NLS-1$
     *      ss.updateAllReferences();
     *      dataStore.addAll(ss.query(ts, trace));
     *  }
     * </pre>
     * <p>
     * will generate the following trace
     *
     * <pre>
     * {@code trace output}
     *  INFO: {"ts":12345,"ph":"B",tid:1,"name:Perform Query"}
     *  INFO: {"ts":"12366,"ph":"E","tid":1}
     * </pre>
     *
     * @author Matthew Khouzam
     *
     */
    public static class ScopeLog implements AutoCloseable {

        private final long fThreadId;
        private final Logger fLog;
        private final Level fLevel;

        /**
         * Scope logger constructor
         *
         * @param log
         *            the logger
         * @param level
         *            the log level see {@link Level}
         * @param label
         *            The label of the event pair
         * @param args
         *            the messages to pass, should be in pairs key, value, key2,
         *            value2.... typically arguments
         */
        public ScopeLog(Logger log, Level level, String label, Object... args) {
            long time = System.nanoTime();
            fLog = log;
            fLevel = level;
            fThreadId = Thread.currentThread().getId();
            fLog.log(fLevel, (() -> {
                StringBuilder sb = new StringBuilder();
                sb.append('{');
                appendCommon(sb, 'B', time, fThreadId);
                appendName(sb, label);
                appendArgs(sb, args);
                sb.append('}');
                return sb.toString();
            }));
        }

        @Override
        public void close() {
            long time = System.nanoTime();
            fLog.log(fLevel, (() -> {
                StringBuilder sb = new StringBuilder();
                sb.append('{');
                return appendCommon(sb, 'E', time, fThreadId).append('}').toString();
            }));
        }
    }

    /**
     * Trace Object Destruction, logs the begining of an object's life cycle.
     * Typically one can put this in the object's Dispose(). However if an
     * object is mutable, it can be tracked through phases with this method,
     * then the object can be re-used, however, the resulting analyses may be
     *
     * @param logger
     *            The Logger
     * @param level
     *            The {@link Level} of this event.
     * @param item
     *            the Object to trace
     */
    public static void traceObjectCreation(Logger logger, Level level, Object item) {
        long time = System.nanoTime();
        long threadId = Thread.currentThread().getId();
        logger.log(level, () -> {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            appendCommon(sb, 'N', time, threadId);
            appendName(sb, item.getClass().getSimpleName());
            appendId(sb, System.identityHashCode(item));
            return sb.append('}').toString();
        });
    }

    /**
     * Trace Object Destruction, logs the end of an object's life cycle.
     * Typically one can put this in the object's Dispose(). However if an
     * object is mutable, it can be tracked through phases with this method,
     * then the object can be re-used, however, the resulting analyses may be
     *
     * @param logger
     *            The Logger
     * @param level
     *            The {@link Level} of this event.
     * @param item
     *            the Object to trace
     */
    public static void traceObjectDestruction(Logger logger, Level level, Object item) {
        long time = System.nanoTime();
        long threadId = Thread.currentThread().getId();
        logger.log(level, () -> {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            appendCommon(sb, 'D', time, threadId);
            appendName(sb, item.getClass().getSimpleName());
            appendId(sb, System.identityHashCode(item));
            return sb.append('}').toString();
        });
    }

    /**
     * Asynchronous events are used to specify asynchronous operations, such as
     * an asynchronous (or synchronous) draw, or a network operation. Call this
     * method at the beginning of such an operation.
     *
     * @param logger
     *            The Logger
     * @param level
     *            The {@link Level} of this event.
     * @param name
     *            The name of the asynchronous message
     * @param category
     *            the category of the asynchronous event
     * @param id
     *            The unique ID of a transaction
     * @param args
     *            Additional arguments to log
     */
    public static void traceAsyncStart(Logger logger, Level level, @Nullable String name, @Nullable String category, int id, Object... args) {
        long time = System.nanoTime();
        long threadId = Thread.currentThread().getId();
        logger.log(level, () -> {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            appendCommon(sb, 'b', time, threadId);
            appendName(sb, name);
            appendCategory(sb, category);
            appendId(sb, id);
            return appendArgs(sb, args).append('}').toString();
        });
    }

    /**
     * Asynchronous events are used to specify asynchronous operations, such as
     * an asynchronous (or synchronous) draw, or a network operation. Call this
     * method to augment the asynchronous event with nested information.
     *
     * @param logger
     *            The Logger
     * @param level
     *            The {@link Level} of this event.
     * @param name
     *            The name of the asynchronous message
     * @param category
     *            the category of the asynchronous event
     * @param id
     *            The unique ID of a transaction
     * @param args
     *            Additional arguments to log
     */
    public static void traceAsyncNested(Logger logger, Level level, @Nullable String name, @Nullable String category, int id, Object... args) {
        long time = System.nanoTime();
        long threadId = Thread.currentThread().getId();
        logger.log(level, () -> {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            appendCommon(sb, 'n', time, threadId);
            appendName(sb, name);
            appendCategory(sb, category);
            appendId(sb, id);
            return appendArgs(sb, args).append('}').toString();
        });
    }

    /**
     * Asynchronous events are used to specify asynchronous operations, such as
     * an asynchronous (or synchronous) draw, or a network operation. Call this
     * method at the end of such an operation.
     *
     * @param logger
     *            The Logger
     * @param level
     *            The {@link Level} of this event.
     * @param name
     *            The name of the asynchronous message
     * @param category
     *            the category of the asynchronous event
     * @param id
     *            The unique ID of a transaction
     * @param args
     *            Additional arguments to log
     */
    public static void traceAsyncEnd(Logger logger, Level level, @Nullable String name, @Nullable String category, int id, Object... args) {
        long time = System.nanoTime();
        long threadId = Thread.currentThread().getId();
        logger.log(level, () -> {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            appendCommon(sb, 'e', time, threadId);
            appendName(sb, name);
            appendCategory(sb, category);
            appendId(sb, id);
            return appendArgs(sb, args).append('}').toString();
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /*
     * USE ME FIRST
     */
    private static StringBuilder appendCommon(StringBuilder appendTo, char phase, long time, long threadId) {
        writeObject(appendTo, TIMESTAMP, time).append(','); // $NON-NLS-1$
        writeObject(appendTo, PHASE, phase).append(',');
        return writeObject(appendTo, TID, threadId); // $NON-NLS-1$
    }

    private static StringBuilder appendName(StringBuilder sb, @Nullable String name) {
        if (name != null) {
            sb.append(',');
            writeObject(sb, NAME, name);
        }
        return sb;
    }

    private static StringBuilder appendCategory(StringBuilder sb, @Nullable String category) {
        if (category != null) {
            sb.append(',');
            writeObject(sb, CATEGORY, category);
        }
        return sb;
    }

    private static StringBuilder appendId(StringBuilder sb, int id) {
        return sb.append(',')
                .append('"')
                .append(ID)
                .append("\":\"0x") //$NON-NLS-1$
                .append(Integer.toHexString(id))
                .append('"');
    }

    private static StringBuilder appendArgs(StringBuilder sb, Object... args) {
        if (args.length > 0) {
            sb.append(',')
                    .append('"')
                    .append(ARGS)
                    .append('"')
                    .append(':');
            getArgs(sb, args);
        }
        return sb;
    }

    private static StringBuilder getArgs(StringBuilder appendTo, Object[] data) {
        Set<String> tester = new HashSet<>();
        appendTo.append('{');
        if (data.length == 1) {
            // not in contract, but let's assume here that people are still new
            // at this
            appendTo.append("\"msg\":\"").append(data[0]).append('"'); //$NON-NLS-1$
        } else {
            if (data.length % 2 != 0) {
                throw new IllegalArgumentException(
                        ARGS_ERROR_MESSAGE + "an odd number of messages" + Arrays.asList(data).toString()); //$NON-NLS-1$
            }
            for (int i = 0; i < data.length - 1; i += 2) {
                Object value = String.valueOf(data[i + 1]);
                String keyVal = String.valueOf(data[i]);
                if (tester.contains(keyVal)) {
                    throw new IllegalArgumentException(ARGS_ERROR_MESSAGE + "an duplicate field names : " + keyVal); //$NON-NLS-1$
                }
                tester.add(keyVal);
                if (i > 0) {
                    appendTo.append(',');
                }
                writeObject(appendTo, keyVal, NonNullUtils.checkNotNull(value));
            }
        }

        return appendTo.append('}');
    }

    private static StringBuilder writeObject(StringBuilder appendTo, Object key, Object value) {
        appendTo.append('"').append(key).append('"').append(':');
        if (value instanceof Number) {
            appendTo.append(value);
        } else {
            appendTo.append('"').append(value).append('"');
        }
        return appendTo;
    }

}