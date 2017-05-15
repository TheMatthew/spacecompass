package org.eclipse.tracecompass.internal.analysis.chromium.core.counters;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.analysis.chromium.core.event.TraceEventEvent;
import org.eclipse.tracecompass.internal.analysis.chromium.core.event.TraceEventLookup;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Metrics counter
 *
 * @author Matthew Khouzam
 *
 */
public class ChromiumCounterProvider extends AbstractTmfStateProvider {

    private static final ITmfEventType COUNT_EVENT = TraceEventLookup.get('C');
    private static final @NonNull String ID = "org.eclipse.tracecompass.internal.analysis.chromium.core.counters.id";

    public ChromiumCounterProvider(@NonNull ITmfTrace trace) {
        super(trace, ID);
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new ChromiumCounterProvider(getTrace());
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        if (!(event instanceof TraceEventEvent)) {
            return;
        }
        TraceEventEvent traceEvent = (TraceEventEvent) event;
        if (!Objects.equals(traceEvent.getType(), COUNT_EVENT)) {
            return;
        }
        ITmfStateSystemBuilder ssb = getStateSystemBuilder();
        if (ssb == null) {
            return;
        }
        Map<@NonNull String, @NonNull Object> args = traceEvent.getField().getArgs();
        if (args != null) {
            for (Entry<@NonNull String, @NonNull Object> arg : args.entrySet()) {

                Object value = arg.getValue();
                double doubleValue = Double.parseDouble(value.toString());
                TmfStateValue sv = TmfStateValue.newValueDouble(doubleValue);
                int quark = ssb.getQuarkAbsoluteAndAdd(traceEvent.getName(), arg.getKey());
                ssb.modifyAttribute(traceEvent.getTimestamp().toNanos(), sv, quark);
            }
        }
    }

}
