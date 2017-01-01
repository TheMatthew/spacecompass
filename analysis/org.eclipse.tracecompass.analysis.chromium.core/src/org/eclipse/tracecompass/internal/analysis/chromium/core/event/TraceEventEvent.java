/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.chromium.core.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.lookup.ITmfCallsite;
import org.eclipse.tracecompass.tmf.core.event.lookup.ITmfSourceLookup;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Trace compass log event
 *
 * @author Matthew Khouzam
 */
public class TraceEventEvent extends TmfEvent implements ITmfSourceLookup {

    private final @Nullable ITmfCallsite fCallsite;
    private final Level fLogLevel;
    private @NonNull final String fName;
    private final TraceEventField fField;

    /**
     * Constructor
     */
    @Deprecated
    public TraceEventEvent() {
        super();
        fCallsite = null;
        fLogLevel = Level.OFF;
        fName = StringUtils.EMPTY;
        fField = new TraceEventField(StringUtils.EMPTY, 0, 'X', null, null, null, Collections.EMPTY_MAP);
    }

    /**
     * Constructor to use
     *
     * @param rank
     *            The event rank
     * @param trace
     *            the event trace
     * @param timestamp
     *            the event timestamp
     * @param name
     *            the event name
     * @param cs
     *            the callsite
     * @param eventType
     *            The event type
     * @param logLevel
     *            the log level
     * @param field
     *            the event type
     */
    public TraceEventEvent(long rank, ITmfTrace trace, ITmfTimestamp timestamp, @NonNull String name, ITmfCallsite cs, ITmfEventType eventType, Level logLevel, TraceEventField field) {
        super(trace, rank, timestamp, eventType, null);
        fCallsite = cs;
        fLogLevel = logLevel;
        fName = name;
        fField = field;
    }

    public TraceEventEvent(ITmfTrace trace, long rank, TraceEventField field) {
        super(trace, rank, TmfTimestamp.fromNanos(field.getTs()), TraceEventLookup.get(field.getPhase()), field.getContent());
        fField = field;
        fName = field.getName();
        fLogLevel = Level.INFO;
        fCallsite = null;
    }

    @Override
    public ITmfEventField getContent() {
        return fField.getContent();
    }

    @Override
    public @NonNull String getName() {
        return fName;
    }

    @Override
    public @Nullable ITmfCallsite getCallsite() {
        return fCallsite;
    }

    /**
     * Parse non-utils based string
     *
     * @param fieldsString
     *            the fields string
     * @return the fields parsed
     */
    public static TraceEventField parseRegular(String fieldsString) {
        // Line looks like this
        //
        // [TimeGraphView:RefreshRequested]
        // viewId=org.eclipse.tracecompass.analysis.os.linux.views.resources
        int split = fieldsString.indexOf(']');
        @NonNull
        String name = "unknown"; //$NON-NLS-1$
        Map<@NonNull String, @NonNull Object> args = new HashMap<>();
        if (split != -1) {
            name = fieldsString.substring(0, split + 1).trim();
            String subst = fieldsString.substring(split + 1);
            if (!subst.isEmpty()) {
                String[] pairs = subst.split(","); //$NON-NLS-1$
                int i = 0;
                for (String pair : pairs) {
                    String[] keys = pair.trim().split("="); //$NON-NLS-1$
                    if (keys.length == 2) {
                        args.put(String.valueOf(keys[0]), String.valueOf(keys[1]));
                    } else if (keys.length == 1) {
                        args.put(createKey(i), String.valueOf(keys[0]));
                        i++;
                    } else if (keys.length > 2) {
                        args.put(createKey(i), pair);
                        i++;
                    }
                }
            }
        }
        return new TraceEventField(name, 0, 'I', null, null, null, args);
    }

    private static @NonNull String createKey(int i) {
        return "msg " + i; //$NON-NLS-1$
    }

    /**
     * Parse a JSON string
     *
     * @param fieldsString
     *            the string
     * @return an event field
     */
    public static TraceEventField parseJson(String fieldsString) {
        // looks like this
        // {"ts":94824347413117,"phase":"B","tid":39,"name":"TimeGraphView:BuildThread","args"={"trace":"django-httpd"}}
        JSONObject root;
        Map<@NonNull String, @NonNull Object> argsMap = new HashMap<>();
        try {
            root = new JSONObject(fieldsString);
            long ts = 0;
            Double tso = root.optDouble(ITraceEventConstants.TIMESTAMP);
            if (Double.isFinite(tso)) {
                ts = (long) (tso * 1000.0);
            }
            char phase = root.optString(ITraceEventConstants.PHASE, "I").charAt(0); //$NON-NLS-1$
            String name = String.valueOf(root.optString(ITraceEventConstants.NAME, 'E' == phase ? "exit" : "unknown")); //$NON-NLS-1$ //$NON-NLS-2$
            Integer tid = root.optInt(ITraceEventConstants.TID, Integer.MIN_VALUE);
            if (tid == Integer.MIN_VALUE) {
                tid = null;
            }
            Object pid = root.opt("pid");
            Double dur = root.optDouble("dur");
            String category = root.optString(ITraceEventConstants.CATEGORY);
            String id = root.optString(ITraceEventConstants.ID);
            JSONObject args = root.optJSONObject(ITraceEventConstants.ARGS);
            if (args != null) {
                Iterator<?> keys = args.keys();
                while (keys.hasNext()) {
                    String key = String.valueOf(keys.next());
                    String value = args.optString(key);
                    argsMap.put("arg/" + key, String.valueOf(value));
                }
            }
            argsMap.put(ITraceEventConstants.TIMESTAMP, ts);
            argsMap.put(ITraceEventConstants.PHASE, phase);
            argsMap.put(ITraceEventConstants.NAME, name);
            if (tid != null) {
                argsMap.put(ITraceEventConstants.TID, tid);
            }
            if (pid != null) {
                argsMap.put(ITraceEventConstants.PID, pid);
            }
            if (Double.isFinite(dur)) {
                argsMap.put(ITraceEventConstants.DURATION, dur);
            }
            return new TraceEventField(name, ts, phase, tid, category, id, argsMap);
        } catch (JSONException e1) {
            // invalid, return null and it will fail
        }
        return null;
    }

    /**
     * Get the loglevel of the event
     *
     * @return the log level
     */
    public Level getLevel() {
        return fLogLevel;
    }

    /**
     * Get the fields of the event
     *
     * @return the fields of the event
     */
    public TraceEventField getField() {
        return fField;
    }
}