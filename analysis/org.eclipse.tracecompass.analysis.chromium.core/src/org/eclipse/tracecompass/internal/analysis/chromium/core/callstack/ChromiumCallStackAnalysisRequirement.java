package org.eclipse.tracecompass.internal.analysis.chromium.core.callstack;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAnalysisEventFieldRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfCompositeAnalysisRequirement;

import com.google.common.collect.ImmutableSet;

public class ChromiumCallStackAnalysisRequirement extends TmfCompositeAnalysisRequirement {
    public ChromiumCallStackAnalysisRequirement() {
        super(getSubRequirements(), PriorityLevel.AT_LEAST_ONE);
    }

    private static Collection<TmfAbstractAnalysisRequirement> getSubRequirements() {
        Set<@NonNull String> requiredEventsFields = ImmutableSet.of(
                "dur");

        TmfAnalysisEventFieldRequirement entryReq = new TmfAnalysisEventFieldRequirement(
                "",
                requiredEventsFields);
        return Collections.singleton(entryReq);
    }

}
