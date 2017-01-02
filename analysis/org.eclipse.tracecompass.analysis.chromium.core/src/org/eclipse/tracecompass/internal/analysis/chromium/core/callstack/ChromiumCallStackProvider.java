package org.eclipse.tracecompass.internal.analysis.chromium.core.callstack;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.chromium.core.event.TraceEventEvent;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.callstack.CallStackStateProvider;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Trace event callstack provider
 *
 * @author Matthew Khouzam
 *
 */
public class ChromiumCallStackProvider extends CallStackStateProvider {

    private ITmfTimestamp fSafeTime;

    /**
     * A map of tid/stacks of timestamps
     */
    private Map<Integer, Stack<Long>> fStack = new TreeMap<>();

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
        Integer fieldValue = event.getContent().getFieldValue(Integer.class, "pid"); //$NON-NLS-1$
        return fieldValue == null ? -1 : fieldValue.intValue();
    }

    @Override
    protected long getThreadId(@NonNull ITmfEvent event) {
        Integer fieldValue = event.getContent().getFieldValue(Integer.class, "tid"); //$NON-NLS-1$
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
        if (event instanceof TraceEventEvent && 'B' == (((TraceEventEvent) event).getField().getPhase())) {
            return TmfStateValue.newValueString(event.getName());
        }
        return null;
    }

    @Override
    protected @Nullable ITmfStateValue functionExit(@NonNull ITmfEvent event) {
        if (event instanceof TraceEventEvent && 'E' == (((TraceEventEvent) event).getField().getPhase())) {
            return TmfStateValue.newValueString(event.getName());
        }
        return null;
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
            ITmfStateValue functionBeginName = functionEntry(event);
            if (functionBeginName != null) {
                startHandle(event, ss, timestamp, processName, functionBeginName);
            }
            break;

        case 'X':
            Long duration = traceEvent.getField().getDuration();
            if (duration != null) {
                handleComplete(traceEvent, ss, processName);
            }
            break;

        case 'E':
            /* Check if the event is a function exit */
            ITmfStateValue functionExitState = functionExit(event);
            if (functionExitState != null) {
                endHandle(event, ss, timestamp, processName);
            }
            break;

        case 'b':
            ITmfStateValue functionStartName = functionEntry(event);
            if (functionStartName != null) {
                startHandle(event, ss, timestamp, processName, functionStartName);
            }
            break;
        // $CASES-OMITTED$
        default:
            return;
        }
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

    private void startHandle(ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp, String processName, ITmfStateValue functionEntryName) {
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
    }

    private void endHandle(ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp, String processName) {
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
}
