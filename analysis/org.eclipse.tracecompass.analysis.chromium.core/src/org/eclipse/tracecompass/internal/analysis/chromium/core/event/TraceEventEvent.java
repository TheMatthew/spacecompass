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
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.lookup.ITmfCallsite;
import org.eclipse.tracecompass.tmf.core.event.lookup.ITmfSourceLookup;
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

    private static final double MICRO_TO_NANO = 1000.0;
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
        fField = new TraceEventField(StringUtils.EMPTY, 0, 'X', null, null, null, null, null, Collections.EMPTY_MAP);
    }

    /**
     * Constructor for simple traceEventEvent
     *
     * @param trace
     *            the trace
     * @param rank
     *            the rank
     * @param field
     *            the event field, contains all the needed data
     */
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
                ts = (long) (tso * MICRO_TO_NANO);
            }
            char phase = root.optString(ITraceEventConstants.PHASE, "I").charAt(0); //$NON-NLS-1$
            String name = String.valueOf(root.optString(ITraceEventConstants.NAME, 'E' == phase ? "exit" : "unknown")); //$NON-NLS-1$ //$NON-NLS-2$
            Integer tid = root.optInt(ITraceEventConstants.TID, Integer.MIN_VALUE);
            if (tid == Integer.MIN_VALUE) {
                tid = null;
            }
            Object pid = root.opt(ITraceEventConstants.PID);
            Double duration = root.optDouble(ITraceEventConstants.DURATION);
            if (Double.isFinite(duration)) {
                duration = (duration * MICRO_TO_NANO);
            }
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
            if (Double.isFinite(duration)) {
                argsMap.put(ITraceEventConstants.DURATION, duration);
            }
            return new TraceEventField(name, ts, phase, pid, tid, category, id, duration, argsMap);
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