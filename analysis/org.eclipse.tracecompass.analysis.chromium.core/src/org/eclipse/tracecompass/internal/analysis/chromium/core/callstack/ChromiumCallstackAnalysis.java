package org.eclipse.tracecompass.internal.analysis.chromium.core.callstack;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Collection;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.chromium.core.trace.ChromiumTrace;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.callstack.CallStackAnalysis;
import org.eclipse.tracecompass.tmf.core.callstack.TimeGraphVertex;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableSet;

/**
 * Call-stack analysis to populate the TMF CallStack View from Trace Event
 * events
 *
 * @author Matthew khouzam
 */
public class ChromiumCallstackAnalysis extends CallStackAnalysis {

    /**
     * ID
     */
    public static final String ID = "org.eclipse.tracecompass.analysis.chromium.callstack"; //$NON-NLS-1$

    private @Nullable Set<@NonNull TmfAbstractAnalysisRequirement> fAnalysisRequirements = null;

    @Override
    public boolean setTrace(ITmfTrace trace) throws TmfAnalysisException {
        if (!(trace instanceof ChromiumTrace)) {
            return false;
        }
        return super.setTrace(trace);
    }

    @Override
    protected ChromiumTrace getTrace() {
        return (ChromiumTrace) super.getTrace();
    }

    @Override
    protected ITmfStateProvider createStateProvider() {
        ChromiumCallStackProvider chromiumCallStackProvider = new ChromiumCallStackProvider(checkNotNull(getTrace()));
        Collection<@NonNull TimeGraphVertex> links = chromiumCallStackProvider.getLinks();
        if (links != null && getCallstackLinks().isEmpty()) {
            setLinks(links);
        }
        return chromiumCallStackProvider;
    }

    @Override
    public @NonNull Iterable<@NonNull TmfAbstractAnalysisRequirement> getAnalysisRequirements() {

        Set<@NonNull TmfAbstractAnalysisRequirement> requirements = fAnalysisRequirements;
        if (requirements == null) {
            requirements = ImmutableSet.of(new ChromiumCallStackAnalysisRequirement());
            fAnalysisRequirements = requirements;
        }
        return requirements;
    }

}
