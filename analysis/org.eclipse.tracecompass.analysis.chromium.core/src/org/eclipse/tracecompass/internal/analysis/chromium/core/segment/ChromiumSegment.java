package org.eclipse.tracecompass.internal.analysis.chromium.core.segment;

import org.eclipse.tracecompass.internal.analysis.chromium.core.event.ChromiumEvent;
import org.eclipse.tracecompass.segmentstore.core.ISegment;

public class ChromiumSegment implements ISegment {

    /**
     *
     */
    private static final long serialVersionUID = 1338806766704581907L;
    private final ChromiumEvent fEvent;

    public static ChromiumSegment create(ChromiumEvent event) {
        if (event != null && event.getContent().getFieldValue(Long.class, "dur") != -1) {
            return new ChromiumSegment(event);
        }
        return null;
    }

    private ChromiumSegment(ChromiumEvent event) {
        fEvent = event;

    }

    @Override
    public long getStart() {
        return fEvent.getTimestamp().toNanos();
    }

    @Override
    public long getEnd() {
        Long fieldValue = fEvent.getContent().getFieldValue(Long.class, "dur");
        if (fieldValue == null) {
            throw new IllegalStateException("Duration should exist");
        }
        return fEvent.getTimestamp().toNanos() + fieldValue;
    }

    public String getType() {
        return fEvent.getName();
    }
}
