package org.eclipse.tracecompass.internal.analysis.chromium.core.event;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;

import com.google.common.base.Joiner;

/**
 * @author matthew
 *
 */
public class ChromiumFields implements ITmfEventField {

    public enum Phase {
        Begin, End, Complete, Instant, Counter, NestableStart, NestableInstant, NestableEnd, Start, Step, Sample, Created, Snapshot, Destroyed, Metadata, Global, Process, Mark, ClockSync
    }

    private final String fName;
    private final Integer fTid;
    private final String fPid;
    private final Character fPh;
    private final Long fDuration;
    private final Integer fId;
    private final Collection<String> fCat;

    public ChromiumFields(String name, String cat, int tid, String pid, char ph, int id, long duration) {
        super();
        fName = name;
        fCat = cat == null ? null : Arrays.asList(cat.split(","));
        fTid = tid;
        fPid = pid != null ? pid.intern() : null;
        fPh = ph;
        fDuration = duration;
        fId = id;
    }

    private static final Phase[] PHASES = new Phase[256];
    private ITmfEventField[] fFields;
    static {
        PHASES['B'] = Phase.Begin;
        PHASES['E'] = Phase.End;
        PHASES['X'] = Phase.Complete;
        PHASES['i'] = Phase.Instant;
        PHASES['I'] = Phase.Instant;// Deprecated
        PHASES['C'] = Phase.Counter;
        PHASES['b'] = Phase.NestableStart;
        PHASES['n'] = Phase.NestableInstant;
        PHASES['e'] = Phase.NestableEnd;
        PHASES['s'] = Phase.Start;
        PHASES['t'] = Phase.Step;
        PHASES['e'] = Phase.End;
        PHASES['P'] = Phase.Sample;
        PHASES['N'] = Phase.Created;
        PHASES['O'] = Phase.Snapshot;
        PHASES['D'] = Phase.Destroyed;
        PHASES['M'] = Phase.Metadata;
        PHASES['V'] = Phase.Global;
        PHASES['v'] = Phase.Process;
        PHASES['R'] = Phase.Mark;
        PHASES['c'] = Phase.ClockSync;

    }

    @Override
    public <T> @Nullable T getFieldValue(Class<T> type, String... path) {
        if (path.length > 1) {
            return null;
        }
        String fieldName = path[0];
        if (fieldName.equals("tid") && type.equals(Integer.class)) {
            return (@Nullable T) fTid;
        }
        if (fieldName.equals("pid") && type.equals(String.class)) {
            return (@Nullable T) fPid;
        }
        if (fieldName.equals("id") && type.equals(Integer.class)) {
            return (@Nullable T) fId;
        }
        if (fieldName.equals("dur") && type.equals(Long.class)) {
            return (@Nullable T) fDuration;
        }
        if (fieldName.equals("ph") && type.equals(Phase.class)) {
            return (@Nullable T) PHASES[fPh];
        }
        if (fieldName.equals("id") && type.equals(Integer.class)) {
            return (@Nullable T) fId;
        }
        return null;
    }

    @Override
    public @NonNull String getName() {
        return fName;
    }

    @Override
    public Object getValue() {
        return getFields();
    }

    @Override
    public String getFormattedValue() {
        return Joiner.on(',').skipNulls().join(getFields());
    }

    @Override
    public @NonNull List<@NonNull String> getFieldNames() {
        return ChromiumType.FIELD_NAMES;
    }

    @Override
    public @NonNull List<? extends ITmfEventField> getFields() {
        if (fFields == null) {
            fFields = new ITmfEventField[getFieldNames().size()];
            fFields[0] = new TmfEventField("tid", fTid, null);
            fFields[1] = fPid == null ? null : new TmfEventField("pid", fPid, null);
            Phase value = PHASES[fPh];
            fFields[2] = value == null ? null : new TmfEventField("ph", value, null);
            fFields[3] = fId == -1 ? null : new TmfEventField("id", fId, null);
            fFields[4] = fCat == null ? null : new TmfEventField("cat", fCat, null);
            fFields[5] = fDuration == -1 ? null : new TmfEventField("dur", fDuration, null);
        }
        return Arrays.asList(fFields);
    }

    @Override
    public ITmfEventField getField(String @NonNull... path) {
        if (path.length == 1) {
            getFields();
            int indexOf = getFieldNames().indexOf(path[0]);
            if (indexOf >= 0 && indexOf < fFields.length) {
                return fFields[indexOf];
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return getFormattedValue();
    }

    public Phase getPh() {
        return PHASES[fPh];
    }

    public char getPhRaw() {
        return fPh;
    }

    public Long getDuration() {
        return fDuration;
    }
}
