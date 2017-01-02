/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.segmentstore.core.arraylist;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.segmentstore.core.BasicSegment;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.SegmentComparators;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

/**
 * Implementation of an {@link ISegmentStore} using one in-memory
 * {@link ArrayList}. This relatively simple implementation holds everything in
 * memory, and as such cannot contain too much data.
 *
 * The LazyArrayListStore itself is {@link Iterable}, and its iteration order
 * will be by ascending order of start times. For segments with identical start
 * times, the secondary comparator will be the end time. If even those are
 * equal, it will defer to the segments' natural ordering (
 * {@link ISegment#compareTo}).
 *
 * This structure sorts in a lazy way to allow faster insertions. However, if
 * the structure is out of order, the next read (getting intersecting elements,
 * iterating...) will perform a sort. It may have inconsistent performance, but
 * should be faster at building when receiving shuffled datasets than the
 * {@link ArrayListStore}.
 *
 * Removal operations are not supported.
 *
 * @param <E>
 *            The type of segment held in this store
 *
 * @author Matthew Khouzam
 */
public class LazyArrayListStore<@NonNull E extends ISegment> implements ISegmentStore<E> {

    private final Comparator<E> COMPARATOR = Ordering.from(SegmentComparators.INTERVAL_START_COMPARATOR)
            .compound(SegmentComparators.INTERVAL_END_COMPARATOR);

    private final ReentrantLock fLock = new ReentrantLock(false);

    private final List<E> fStore = new ArrayList<>();

    private @Nullable transient Iterable<E> fLastSnapshot = null;

    private volatile boolean fDirty = false;

    /**
     * Constructor
     */
    public LazyArrayListStore() {
        // do nothing
    }

    /**
     * Constructor
     *
     * @param array
     *            an array of elements to wrap in the segment store
     *
     */
    public LazyArrayListStore(Object[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] instanceof ISegment) {
                E element = (E) array[i];
                setDirtyIfNeeded(element);
                fStore.add(element);
            }
        }
        if (fDirty) {
            sortStore();
        }
    }

    private void setDirtyIfNeeded(@NonNull E value) {
        if (!fStore.isEmpty() && COMPARATOR.compare(fStore.get(size() - 1), value) > 0) {
            fDirty = true;
        }
    }
    // ------------------------------------------------------------------------
    // Methods from Collection
    // ------------------------------------------------------------------------

    @Override
    public Iterator<E> iterator() {
        fLock.lock();
        try {
            if (fDirty) {
                sortStore();
            }
            Iterable<E> lastSnapshot = fLastSnapshot;
            if (lastSnapshot == null) {
                lastSnapshot = ImmutableList.copyOf(fStore);
                fLastSnapshot = lastSnapshot;
            }
            return checkNotNull(lastSnapshot.iterator());
        } finally {
            fLock.unlock();
        }
    }

    /**
     * DO NOT CALL FROM OUTSIDE OF A LOCK!
     */
    private void sortStore() {
        fStore.sort(COMPARATOR);
        fDirty = false;
    }

    @Override
    public boolean add(@Nullable E val) {
        if (val == null) {
            throw new IllegalArgumentException("Cannot add null value"); //$NON-NLS-1$
        }

        fLock.lock();
        try {
            setDirtyIfNeeded(val);
            fStore.add(val);
            fLastSnapshot = null;
            return true;
        } finally {
            fLock.unlock();
        }
    }

    @Override
    public int size() {
        fLock.lock();
        try {
            return fStore.size();
        } finally {
            fLock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        fLock.lock();
        try {
            return fStore.isEmpty();
        } finally {
            fLock.unlock();
        }
    }

    @Override
    public boolean contains(@Nullable Object o) {
        fLock.lock();
        try {
            return fStore.contains(o);
        } finally {
            fLock.unlock();
        }
    }

    @Override
    public boolean containsAll(@Nullable Collection<?> c) {
        fLock.lock();
        try {
            return fStore.containsAll(c);
        } finally {
            fLock.unlock();
        }
    }

    @Override
    public Object[] toArray() {
        fLock.lock();
        try {
            if (fDirty) {
                sortStore();
            }
            return fStore.toArray();
        } finally {
            fLock.unlock();
        }
    }

    @Override
    public <T> T[] toArray(T[] a) {
        fLock.lock();
        try {
            if (fDirty) {
                sortStore();
            }
            return fStore.toArray(a);
        } finally {
            fLock.unlock();
        }
    }

    @Override
    public boolean addAll(@Nullable Collection<? extends E> c) {
        if (c == null) {
            throw new IllegalArgumentException();
        }

        fLock.lock();
        try {
            boolean changed = false;
            for (E elem : c) {
                if (add(elem)) {
                    changed = true;
                }
            }
            return changed;
        } finally {
            fLock.unlock();
        }
    }

    @Override
    public void clear() {
        fLock.lock();
        try {
            fStore.clear();
            fLastSnapshot = null;
            fDirty = false;
        } finally {
            fLock.unlock();
        }
    }

    // ------------------------------------------------------------------------
    // Methods added by ISegmentStore
    // ------------------------------------------------------------------------

    @Override
    public Iterable<E> getIntersectingElements(long start, long end) {
        fLock.lock();
        if (fDirty) {
            sortStore();
        }
        try {
            int index = Collections.binarySearch(fStore, new BasicSegment(end, Long.MAX_VALUE));
            index = (index >= 0) ? index : -index - 1;
            return fStore.subList(0, index).stream().filter(element -> !(start > element.getEnd() || end < element.getStart())).collect(Collectors.toList());
        } finally {
            fLock.unlock();
        }
    }

    @Override
    public void dispose() {
        fLock.lock();
        try {
            fStore.clear();
            fDirty = false;
        } finally {
            fLock.unlock();
        }
    }
}
