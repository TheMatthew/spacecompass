package org.eclipse.tracecompass.internal.analysis.chromium.core.callstack;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.chromium.core.event.ITraceEventConstants;
import org.eclipse.tracecompass.internal.analysis.chromium.core.event.TraceEventEvent;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.callstack.CallStackStateProvider;
import org.eclipse.tracecompass.tmf.core.callstack.TimeGraphVertex;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Trace event callstack provider
 *
 * Has links, so we have a tmfGraph.
 *
 * @author Matthew Khouzam
 *
 */
public class ChromiumCallStackProvider extends CallStackStateProvider {
    /**
     * Link builder between events
     *
     * @author Matthew Khouzam
     */
    private final class VertexBuilder {

        private long fSrcTime = Long.MAX_VALUE;
        private int fSrcAttr;
        private int fDst;
        private long fDur;

        public long getTime() {
            return fSrcTime;
        }

        public TimeGraphVertex build() {
            return new TimeGraphVertex(fSrcAttr, fDst, fSrcTime, fDur);
        }

    }

    private ITmfTimestamp fSafeTime;

    private final Map<String, VertexBuilder> fLinks = new HashMap<>();

    private final Collection<@NonNull TimeGraphVertex> fCollection = new TreeSet<>();

    /**
     * A map of tid/stacks of timestamps
     */
    private final Map<Integer, Stack<Long>> fStack = new TreeMap<>();

    /**
     * Constructor
     *
     * @param trace
     *            the trace to follow
     */
    public ChromiumCallStackProvider(@NonNull ITmfTrace trace) {
        super(trace);
        ITmfStateSystemBuilder stateSystemBuilder = getStateSystemBuilder();
        if (stateSystemBuilder != null) {
            int quark = stateSystemBuilder.getQuarkAbsoluteAndAdd("dummy entry to make gpu entries work"); //$NON-NLS-1$
            stateSystemBuilder.modifyAttribute(0, TmfStateValue.newValueInt(0), quark);
        }
        fSafeTime = trace.getStartTime();
    }

    @Override
    protected @Nullable String getProcessName(@NonNull ITmfEvent event) {
        String pName = event.getContent().getFieldValue(String.class, "pid"); //$NON-NLS-1$

        if (pName == null) {
            int processId = getProcessId(event);
            pName = (processId == UNKNOWN_PID) ? UNKNOWN : Integer.toString(processId);
        }

        return pName;
    }

    @Override
    protected int getProcessId(@NonNull ITmfEvent event) {
        Integer fieldValue = event.getContent().getFieldValue(Integer.class, ITraceEventConstants.PID);
        return fieldValue == null ? -1 : fieldValue.intValue();
    }

    @Override
    protected long getThreadId(@NonNull ITmfEvent event) {
        Integer fieldValue = event.getContent().getFieldValue(Integer.class, ITraceEventConstants.TID);
        return fieldValue == null ? -1 : fieldValue.intValue();

    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public @NonNull CallStackStateProvider getNewInstance() {
        return new ChromiumCallStackProvider(getTrace());
    }

    @Override
    protected boolean considerEvent(@NonNull ITmfEvent event) {
        return (event instanceof TraceEventEvent);
    }

    @Override
    protected @Nullable ITmfStateValue functionEntry(@NonNull ITmfEvent event) {
        if (event instanceof TraceEventEvent && isEntry(event)) {
            return TmfStateValue.newValueString(event.getName());
        }
        return null;
    }

    private static boolean isEntry(ITmfEvent event) {
        char phase = ((TraceEventEvent) event).getField().getPhase();
        return 'B' == phase || phase == 's';
    }

    @Override
    protected @Nullable ITmfStateValue functionExit(@NonNull ITmfEvent event) {
        if (event instanceof TraceEventEvent && isExit(event)) {
            return TmfStateValue.newValueString(event.getName());
        }
        return null;
    }

    private static boolean isExit(ITmfEvent event) {
        char phase = ((TraceEventEvent) event).getField().getPhase();
        return 'E' == phase || 'f' == phase;
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        if (!considerEvent(event)) {
            return;
        }
        ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());

        /* Check if the event is a function entry */
        long timestamp = event.getTimestamp().toNanos();
        for (Entry<Integer, Stack<Long>> stackEntry : fStack.entrySet()) {
            Stack<Long> stack = stackEntry.getValue();
            if (!stack.isEmpty()) {
                Long closeCandidate = stack.pop();
                while (closeCandidate != null && closeCandidate < timestamp) {
                    ss.popAttribute(closeCandidate, stackEntry.getKey());
                    closeCandidate = (stack.isEmpty()) ? null : stack.pop();
                }
                if (closeCandidate != null) {
                    stack.push(closeCandidate);
                }
            }
        }
        TraceEventEvent traceEvent = (TraceEventEvent) event;
        String processName = getProcessName(event);
        char ph = traceEvent.getField().getPhase();
        switch (ph) {
        case 'B':
            handleStart(event, ss, timestamp, processName);
            break;
        case 's': {
            int attr = handleStart(event, ss, timestamp, processName);
            String id = traceEvent.getField().getId();
            VertexBuilder link = fLinks.get(id);
            if (link != null) {
                if (link.getTime() == Long.MAX_VALUE) {
                    link.fDur = 0;
                    link.fSrcTime = traceEvent.getTimestamp().toNanos();
                    link.fDst = attr;
                    fCollection.add(link.build());
                    VertexBuilder value = new VertexBuilder();
                    value.fSrcAttr = attr;
                    fLinks.put(id, value);
                } else {
                    /*
                     * start time = time
                     *
                     * end time = traceEvent.getTimestamp().toNanos()
                     */
                    link.fDur = traceEvent.getTimestamp().toNanos() - link.fSrcTime;
                    link.fDst = attr;
                    fCollection.add(link.build());
                    VertexBuilder value = new VertexBuilder();
                    value.fSrcAttr = attr;
                    fLinks.put(id, value);
                }
            } else {
                VertexBuilder newLink = new VertexBuilder();
                newLink.fSrcAttr = attr;
                fLinks.put(id, newLink);
            }
        }
            break;

        case 'X':
            Long duration = traceEvent.getField().getDuration();
            if (duration != null) {
                handleComplete(traceEvent, ss, processName);
            }
            break;

        case 'E': {
            handleEnd(event, ss, timestamp, processName);
            break;
        }
        case 'f': {
            handleEnd(event, ss, timestamp, processName);
            {
                String id = traceEvent.getField().getId();
                VertexBuilder link = fLinks.get(id);
                if (link != null) {
                    if (link.getTime() == Long.MAX_VALUE) {
                        link.fSrcTime = traceEvent.getTimestamp().toNanos();
                    }
                }
            }
            break;
        }
        case 'b':
            handleStart(event, ss, timestamp, processName);
            break;
        // $CASES-OMITTED$
        default:
            return;
        }
    }

    private int handleStart(ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp, String processName) {
        ITmfStateValue functionBeginName = functionEntry(event);
        if (functionBeginName != null) {
            return startHandle(event, ss, timestamp, processName, functionBeginName);
        }
        return -1;
    }

    private int handleEnd(ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp, String processName) {
        /* Check if the event is a function exit */
        ITmfStateValue functionExitState = functionExit(event);
        if (functionExitState != null) {
            return endHandle(event, ss, timestamp, processName);
        }
        return -1;
    }

    /**
     * This handles phase "complete" elements. They arrive by end time first,
     * some some flipping is being performed.
     *
     * @param event
     * @param ss
     * @param processName
     */
    private void handleComplete(TraceEventEvent event, ITmfStateSystemBuilder ss, String processName) {

        ITmfTimestamp timestamp = event.getTimestamp();
        fSafeTime = fSafeTime.compareTo(timestamp) > 0 ? fSafeTime : timestamp;
        String processName_ = processName;
        if (processName_ == null) {
            int processId = getProcessId(event);
            processName_ = (processId == UNKNOWN_PID) ? UNKNOWN : Integer.toString(processId).intern();
        }
        int processQuark = ss.getQuarkAbsoluteAndAdd(PROCESSES, processName_);
        long startTime = event.getTimestamp().toNanos();
        long end = startTime;
        Long duration = event.getField().getDuration();
        if (duration != null) {
            end += duration;
        }
        String threadName = getThreadName(event);
        long threadId = getThreadId(event);
        if (threadName == null) {
            threadName = Long.toString(threadId).intern();
        }
        int threadQuark = ss.getQuarkRelativeAndAdd(processQuark, threadName);

        int callStackQuark = ss.getQuarkRelativeAndAdd(threadQuark, CALL_STACK);
        ITmfStateValue functionEntry = TmfStateValue.newValueString(event.getName());
        ss.pushAttribute(startTime, functionEntry, callStackQuark);
        Stack<Long> stack = fStack.get(callStackQuark);
        if (stack == null) {
            stack = new Stack<>();
            fStack.put(callStackQuark, stack);
        }
        stack.push(end);

    }

    private int startHandle(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp, String processName, ITmfStateValue functionEntryName) {
        int processQuark = ss.getQuarkAbsoluteAndAdd(PROCESSES, processName);

        String threadName = getThreadName(event);
        long threadId = getThreadId(event);
        if (threadName == null) {
            threadName = Long.toString(threadId);
        }
        int threadQuark = ss.getQuarkRelativeAndAdd(processQuark, threadName);

        int callStackQuark = ss.getQuarkRelativeAndAdd(threadQuark, CALL_STACK);
        ITmfStateValue value = functionEntryName;
        ss.pushAttribute(timestamp, value, callStackQuark);
        // FIXME: BIG FAT SMELLY HACK
        return ss.queryOngoingState(callStackQuark).unboxInt() + callStackQuark;

    }

    private int endHandle(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp, String processName) {
        String pName = processName;

        if (pName == null) {
            int processId = getProcessId(event);
            pName = (processId == UNKNOWN_PID) ? UNKNOWN : Integer.toString(processId);
        }
        String threadName = getThreadName(event);
        if (threadName == null) {
            threadName = Long.toString(getThreadId(event));
        }
        int quark = ss.getQuarkAbsoluteAndAdd(PROCESSES, pName, threadName, CALL_STACK);
        ss.popAttribute(timestamp, quark);
        // FIXME: BIG FAT SMELLY HACK
        return ss.queryOngoingState(quark).unboxInt() + quark;
    }

    @Override
    public void done() {
        ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());
        for (Entry<Integer, Stack<Long>> stackEntry : fStack.entrySet()) {
            Stack<Long> stack = stackEntry.getValue();
            if (!stack.isEmpty()) {
                Long closeCandidate = stack.pop();
                while (closeCandidate != null) {
                    ss.popAttribute(closeCandidate, stackEntry.getKey());
                    closeCandidate = (stack.isEmpty()) ? null : stack.pop();
                }
            }
        }
        super.done();
    }

    protected Collection<@NonNull TimeGraphVertex> getLinks() {
        return fCollection;
    }
}
