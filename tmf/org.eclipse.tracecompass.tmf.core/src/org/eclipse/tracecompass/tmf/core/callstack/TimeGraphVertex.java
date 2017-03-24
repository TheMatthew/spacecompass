package org.eclipse.tracecompass.tmf.core.callstack;

import java.nio.ByteBuffer;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

/**
 * A model element to make a time link
 *
 * TODO: make into a segment?
 *
 * @author Matthew Khouzam
 * @since 2.3
 */
public final class TimeGraphVertex implements Comparable<TimeGraphVertex> {

    /**
     * Size taken on a disk
     */
    public static final int SIZE = 24;

    /**
     * Factory helper
     *
     * @param bb
     *            a {@link ByteBuffer} input, must have at least SIZE free
     * @return a TimeGraphVertex unless there is not enough data, in which case,
     *         null
     */
    public static @Nullable TimeGraphVertex create(ByteBuffer bb) {
        if (bb.remaining() < SIZE) {
            return null;
        }
        int src = bb.getInt();
        int dst = bb.getInt();
        long time = bb.getLong();
        long dur = bb.getLong();
        return new TimeGraphVertex(src, dst, time, dur);
    }

    private final int fSrc;
    private final int fDst;
    private final long fTime;
    private final long fDuration;

    /**
     * Constructor
     *
     * @param src
     *            source attribute
     * @param dst
     *            destination attribute
     * @param time
     *            start time
     * @param duration
     *            duration
     */
    public TimeGraphVertex(int src, int dst, long time, long duration) {
        fSrc = src;
        fDst = dst;
        fTime = time;
        fDuration = duration;

    }

    /**
     * Source ID
     *
     * @return source
     */
    public int getSrc() {
        return fSrc;
    }

    /**
     * Destination ID
     *
     * @return destination
     */
    public int getDst() {
        return fDst;
    }

    /**
     * Time location
     *
     * @return time
     */
    public long getTime() {
        return fTime;
    }

    /**
     * Duration
     *
     * @return duration
     */
    public long getDuration() {
        return fDuration;
    }

    @Override
    public int compareTo(TimeGraphVertex o) {
        int compare = Long.compare(fTime, o.fTime);
        if (compare == 0) {
            compare = Long.compare(fDuration, o.fDuration);
        }
        if (compare == 0) {
            compare = Integer.compare(fSrc, o.fSrc);
        }
        if (compare == 0) {
            compare = Integer.compare(fDst, o.fDst);
        }
        return compare;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fDst, fSrc, fTime, fDuration);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TimeGraphVertex other = (TimeGraphVertex) obj;
        return ((fDst == other.fDst) && (fDuration == other.fDuration) && (fSrc == other.fSrc) && (fTime == other.fTime));
    }

    /**
     * Serializer, returns a ByteBuffer, filled and reset
     *
     * @return the bytebuffer
     */
    public ByteBuffer serialize() {
        byte[] data = new byte[SIZE];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.putInt(getSrc());
        bb.putInt(getDst());
        bb.putLong(getTime());
        bb.putLong(getDuration());
        bb.flip();
        return bb;
    }
}
