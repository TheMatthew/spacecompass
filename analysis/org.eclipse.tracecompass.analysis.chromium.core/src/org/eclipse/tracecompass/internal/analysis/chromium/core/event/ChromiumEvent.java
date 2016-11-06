package org.eclipse.tracecompass.internal.analysis.chromium.core.event;

import java.io.IOException;

import org.eclipse.tracecompass.internal.analysis.chromium.core.event.ChromiumFields.Phase;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

public final class ChromiumEvent extends TmfEvent {

    public ChromiumEvent(ITmfTrace trace, long rank, ITmfTimestamp ts, ITmfEventType type, ChromiumFields content) {
        super(trace, rank, ts, type, content);
    }

    public static ITmfEvent parse(ITmfTrace trace, long rank, String line)
            throws IOException {
        int tid = -1;
        String pid = null;
        ITmfTimestamp ts = null;
        long duration = -1;
        char ph = '-';
        String name = null;
        String cat = null;
        int id = -1;
        if (line == null) {
            return null;
        }
        int start = line.indexOf('{');
        int end = line.lastIndexOf('}');
        if (start == -1 || end == -1) {
            return null;
        }
        String tokenString = line.substring(start + 1, end);
        String[] tokens = tokenString.split(",");

        for (String token : tokens) {
            String[] field = token.split(":"); //$NON-NLS-1$
            if (field.length != 2) {
                continue;
            }
            String fieldName = field[0].substring(1, field[0].length() - 1);
            String value = field[1].trim();
            switch (fieldName) {
            case "dur":
                duration = parseTs(value);
                break;
            case "ts":
                ts = TmfTimestamp.fromNanos(parseTs(value));
                break;
            case "tid":
                tid = getInt(value);
                break;
            case "pid":
                pid = value;
                break;
            case "ph": // phase
                ph = getString(value).charAt(0);
                break;
            case "name":
                name = getString(value);
                break;
            case "cat":
                cat = getString(value);
                break;
            case "id":
                id = getInt(value);
                break;
            default:
                throw new IllegalStateException(fieldName + " " + value);
            }
        }

        return new ChromiumEvent(trace, rank, ts, ChromiumType.get(name), new ChromiumFields(name, cat, tid, pid, ph, id, duration));

    }

    private static int getInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
        }
        return -1;
    }

    private static String getString(String value) {
        int first = value.indexOf('"');
        int last = value.lastIndexOf('"');
        if (first == -1 || last == -1) {
            return value;
        }
        return value.substring(first + 1, last);
    }

    protected static long parseTs(String ts) {
        long valInNs = 0;
        int length = ts.length();
        for (int i = 0; i < length; i++) {
            int val = CHAR_ARRAY[ts.charAt(i)];
            if (val == Integer.MIN_VALUE) {
                int countDown = 3;
                while ((i + 1) < length && countDown > 0) {
                    countDown--;
                    i++;
                    val = CHAR_ARRAY[ts.charAt(i)];
                    valInNs *= 10;
                    valInNs += val;
                }
                return valInNs;
            }
            valInNs *= 10;
            valInNs += val;
        }
        return valInNs;
    }

    private static final int CHAR_ARRAY[] = new int[256];
    static {
        CHAR_ARRAY['0'] = 0;
        CHAR_ARRAY['1'] = 1;
        CHAR_ARRAY['2'] = 2;
        CHAR_ARRAY['3'] = 3;
        CHAR_ARRAY['4'] = 4;
        CHAR_ARRAY['5'] = 5;
        CHAR_ARRAY['6'] = 6;
        CHAR_ARRAY['7'] = 7;
        CHAR_ARRAY['8'] = 8;
        CHAR_ARRAY['9'] = 9;
        CHAR_ARRAY['.'] = Integer.MIN_VALUE;
    }

    public ITmfTimestamp getEndTime() {
        Long dur = ((ChromiumFields) getContent()).getDuration();
        if (dur == -1L) {
            dur = 0L;
        }
        return TmfTimestamp.fromNanos(dur + getTimestamp().toNanos());
    }

    public Phase getPhase() {
        return ((ChromiumFields) getContent()).getPh();
    }
}