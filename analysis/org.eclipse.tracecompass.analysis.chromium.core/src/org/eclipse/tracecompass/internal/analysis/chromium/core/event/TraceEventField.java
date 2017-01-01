/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.chromium.core.event;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;

/**
 * Trace Compass Log fields, used as a quick wrapper for Trace compass log data
 *
 * @author Matthew Khouzam
 */
@NonNullByDefault
public class TraceEventField {

    private final long fTs;
    private final char fPhase;
    private final String fName;
    private ITmfEventField fContent;
    private final @Nullable Map<String, Object> fArgs;
    private final @Nullable Integer fTid;
    private final @Nullable String fCategory;
    private final @Nullable String fId;

    /**
     * Constructor
     *
     * @param name
     *            event name
     * @param ts
     *            the timestamp
     * @param phase
     *            the phase of the event
     * @param tid
     *            the threadId
     * @param category
     *            the category
     * @param id
     *            the ID of the event stream
     * @param fields
     *            event fields (arguments)
     */
    public TraceEventField(String name, long ts, char phase, @Nullable Integer tid, @Nullable String category, @Nullable String id, Map<String, Object> fields) {
        fName = name;
        fTid = tid;
        fCategory = category;
        fId = id;
        ITmfEventField[] array = fields.entrySet().stream()
                .map(entry -> new TmfEventField(entry.getKey(), entry.getValue(), null))
                .toArray(ITmfEventField[]::new);
        fContent = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, fields, array);
        fTs = ts;
        fPhase = phase;
        Map<@NonNull String, @NonNull Object> args = fields.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("args."))
                .collect(Collectors.toMap(entry -> entry.getKey().substring(5), Entry::getValue));
        fArgs = args.isEmpty() ? null : args;

    }

    /**
     * Get the event category
     *
     * @return the event category
     */
    public @Nullable String getCategory() {
        return fCategory;
    }

    /**
     * Get the event content
     *
     * @return the event content
     */
    public ITmfEventField getContent() {
        return fContent;
    }

    /**
     * Get the event ID
     *
     * @return the event ID
     */
    public @Nullable String getId() {
        return fId;
    }

    /**
     * Get the name of the event
     *
     * @return the event name
     */
    public String getName() {
        return fName;
    }

    /**
     * Get the phase of the event
     *
     * @return the event phase
     */
    public char getPhase() {
        return fPhase;
    }

    /**
     * Get the TID of the event
     *
     * @return the event TID
     */
    public @Nullable Integer getTid() {
        return fTid;
    }

    /**
     * Get the timestamp
     *
     * @return the timestamp
     */
    public long getTs() {
        return fTs;
    }

    @Nullable
    public Map<String, Object> getArgs() {
        return fArgs;
    }
}
