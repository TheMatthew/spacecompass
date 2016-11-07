package org.eclipse.tracecompass.internal.analysis.chromium.core.segment;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.chromium.core.trace.ChromiumTrace;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;

public class ChromiumSegmentStore implements ISegmentStore<@NonNull ISegment> {

    private ChromiumTrace fTrace;

    public ChromiumSegmentStore(ChromiumTrace trace) {
        fTrace = trace;
    }

    @Override
    public int size() {
        return (int) fTrace.getNbEvents();
    }

    @Override
    public boolean isEmpty() {
        return fTrace.getNbEvents() == 0;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator<@NonNull ISegment> iterator() {
        return new ChromiumIterator(Long.MIN_VALUE, Long.MAX_VALUE, fTrace);
    }

    @Override
    public Object @NonNull [] toArray() {
        return new Object[0];
    }

    @Override
    public <T> T @NonNull [] toArray(T @NonNull [] a) {
        return (T @NonNull []) new ChromiumSegment[0];
    }

    @Override
    public boolean add(ISegment e) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends ISegment> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {
    }

    @Override
    public @NonNull Iterable<@NonNull ISegment> getIntersectingElements(long start, long end) {
        return new ChromiumIterable(start, end, fTrace);
    }

    @Override
    public void dispose() {
        fTrace.dispose();

    }

}
