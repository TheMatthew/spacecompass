package org.eclipse.tracecompass.internal.analysis.chromium.core.segment;

import java.util.Iterator;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.chromium.core.trace.ChromiumTrace;
import org.eclipse.tracecompass.internal.analysis.chromium.core.event.ChromiumEvent;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;

public class ChromiumIterator implements Iterator<@NonNull ISegment> {

    private static final ISegment INVALID = new ISegment() {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        @Override
        public long getStart() {
            return Long.MAX_VALUE;
        }

        @Override
        public long getEnd() {
            return Long.MIN_VALUE;
        }
    };
    private long fEnd;
    private ITmfContext fContext;
    private ISegment fSeg;
    private ChromiumTrace fTrace;

    /**
     * @param start
     * @param end
     * @param trace
     */
    public ChromiumIterator(long start, long end, ChromiumTrace trace) {
        fEnd = end;
        fTrace = trace;
        fSeg = INVALID;
        fContext = trace.seekEvent(TmfTimestamp.fromNanos(start));
        while (INVALID.equals(fSeg) || fSeg.getStart() < start) {
            ChromiumEvent evt = (ChromiumEvent) fTrace.getNext(fContext);
            if (evt != null) {
                fSeg = ChromiumSegment.create(evt);
            } else {
                fSeg = INVALID;
            }
            if (fSeg == null) {
                fSeg = INVALID;
            }
        }
    }

    @Override
    public boolean hasNext() {
        return fSeg != INVALID;
    }

    @Override
    public ISegment next() {
        ISegment seg = fSeg;
        fSeg = INVALID;
        ISegment segment = INVALID;
        while (INVALID.equals(segment)) {
            ChromiumEvent evt = (ChromiumEvent) fTrace.getNext(fContext);
            if (evt == null) {
                fSeg = INVALID;
                return INVALID;
            }
            segment = ChromiumSegment.create(evt);
            if (segment == null) {
                segment = INVALID;
            }
            fSeg = segment;
        }
        if (segment == null || segment.getStart() > fEnd) {
            fSeg = INVALID;
        }
        return seg;
    }

}
