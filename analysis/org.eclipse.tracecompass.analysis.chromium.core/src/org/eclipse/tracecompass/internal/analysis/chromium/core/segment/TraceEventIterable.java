package org.eclipse.tracecompass.internal.analysis.chromium.core.segment;

import java.util.Iterator;

import org.eclipse.tracecompass.analysis.chromium.core.trace.ChromiumTrace;
import org.eclipse.tracecompass.segmentstore.core.ISegment;

public class TraceEventIterable implements Iterable<ISegment> {


    private long fStart;
    private long fEnd;
    private ChromiumTrace fTrace;

    public TraceEventIterable(long start, long end, ChromiumTrace trace) {
        fStart = start;
        fEnd = end;
        fTrace = trace;
    }

    @Override
    public Iterator<ISegment> iterator() {
        return new TraceEventIterator(fStart, fEnd, fTrace);
    }

}
