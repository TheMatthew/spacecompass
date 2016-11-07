package org.eclipse.tracecompass.internal.analysis.chromium.core.statistics;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.chromium.core.trace.ChromiumTrace;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.statistics.AbstractSegmentStatisticsAnalysis;
import org.eclipse.tracecompass.internal.analysis.chromium.core.segment.ChromiumSegment;
import org.eclipse.tracecompass.internal.analysis.chromium.core.segment.ChromiumSegmentProvider;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

public class ChromiumStatisticsAnalysis extends AbstractSegmentStatisticsAnalysis {

    public static final String ID = "org.eclipse.tracecompass.internal.analysis.chromium.core.statistics.analysis"; //$NON-NLS-1$

    @Override
    protected @Nullable String getSegmentType(@NonNull ISegment segment) {
        if(segment instanceof ChromiumSegment){
            return ((ChromiumSegment) segment).getType();
        }
        return "UNKNOWN";
    }

    @Override
    protected @Nullable ISegmentStoreProvider getSegmentProviderAnalysis(@NonNull ITmfTrace trace) {
        if (trace instanceof ChromiumTrace) {
            return new ChromiumSegmentProvider((ChromiumTrace) trace);
        }
        return null;
    }

}
