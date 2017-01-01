package org.eclipse.tracecompass.internal.analysis.chromium.core.segment;

import org.eclipse.tracecompass.internal.analysis.chromium.core.event.TraceEventEvent;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

public class TraceEventSegment implements ISegment {

    /**
     *
     */
    private static final long serialVersionUID = 1338806766704581907L;
    private final ITmfEvent fEvent;

    public static TraceEventSegment create(TraceEventEvent event) {
        if (event != null && event.getContent().getFieldValue(Double.class, "dur") != null) { //$NON-NLS-1$
            return new TraceEventSegment(event);
        }
        return null;
    }

    private TraceEventSegment(TraceEventEvent event) {
        fEvent = event;

    }

    @Override
    public long getStart() {
        return fEvent.getTimestamp().toNanos();
    }

    @Override
    public long getEnd() {
        Double fieldValue = fEvent.getContent().getFieldValue(Double.class, "dur"); //$NON-NLS-1$
        if (fieldValue == null || !Double.isFinite(fieldValue)) {
            throw new IllegalStateException("Duration should exist"); //$NON-NLS-1$
        }
        return (long) (fEvent.getTimestamp().toNanos() + fieldValue * 1000L);
    }

    public String getType() {
        return fEvent.getName();
    }
}
