package org.eclipse.tracecompass.internal.analysis.chromium.core.event;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;

public class ChromiumType implements ITmfEventType {

    private static final Map<String, ChromiumType> TYPES = new HashMap<>();
    private final String fName;
    public static final List<@NonNull String> FIELD_NAMES = Arrays.asList("tid", "pid", "ph", "id", "cat", "dur");

    private ChromiumType(String name) {
        fName = name;

    }

    @Override
    public String getName() {
        return fName;
    }

    @Override
    public @Nullable ITmfEventField getRootField() {
        return null;
    }

    @Override
    public Collection<String> getFieldNames() {
        return FIELD_NAMES;
    }

    @Override
    public String toString() {
        return getName();
    }

    public static ITmfEventType get(String name) {
        ChromiumType type = TYPES.get(name);
        if (type == null) {
            type = new ChromiumType(name);
            TYPES.put(name, type);
        }
        return type;
    }

}
