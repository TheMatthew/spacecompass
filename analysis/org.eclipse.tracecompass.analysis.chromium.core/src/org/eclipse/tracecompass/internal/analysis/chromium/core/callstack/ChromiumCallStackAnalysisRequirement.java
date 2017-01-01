package org.eclipse.tracecompass.internal.analysis.chromium.core.callstack;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.analysis.chromium.core.event.ITraceEventConstants;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAnalysisEventFieldRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfCompositeAnalysisRequirement;

import com.google.common.collect.ImmutableSet;

/**
 * Requirements to run a trace event based analysis
 *
 * @author Matthew Khouzam
 *
 */
public class ChromiumCallStackAnalysisRequirement extends TmfCompositeAnalysisRequirement {

    /**
     * Default constructor
     */
    public ChromiumCallStackAnalysisRequirement() {
        super(getSubRequirements(), PriorityLevel.AT_LEAST_ONE);
    }

    private static Collection<TmfAbstractAnalysisRequirement> getSubRequirements() {
        Set<@NonNull String> requiredEventsFields = ImmutableSet.of(
                ITraceEventConstants.DURATION);

        TmfAnalysisEventFieldRequirement entryReq = new TmfAnalysisEventFieldRequirement(
                StringUtils.EMPTY,
                requiredEventsFields);
        return Collections.singleton(entryReq);
    }

}
