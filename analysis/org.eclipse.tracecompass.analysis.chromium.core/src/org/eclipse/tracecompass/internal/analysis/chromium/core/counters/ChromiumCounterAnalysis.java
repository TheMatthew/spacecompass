package org.eclipse.tracecompass.internal.analysis.chromium.core.counters;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

public class ChromiumCounterAnalysis extends TmfStateSystemAnalysisModule {

    public static final String ID = "org.eclipse.tracecompass.analysis.chromium.core.counters";

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        ITmfTrace trace = getTrace();
        return new ChromiumCounterProvider(NonNullUtils.checkNotNull(trace));
    }

}
