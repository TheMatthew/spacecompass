package org.eclipse.tracecompass.internal.analysis.chromium.core.segment;

import java.util.Collections;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.chromium.core.trace.ChromiumTrace;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.IAnalysisProgressListener;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;

public class TraceEventSegmentProvider implements ISegmentStoreProvider {

    private final TraceEventSegmentStore fCss;

    public TraceEventSegmentProvider(ChromiumTrace trace) {
        fCss = new TraceEventSegmentStore(trace);
    }

    @Override
    public void addListener(@NonNull IAnalysisProgressListener listener) {
    }

    @Override
    public void removeListener(@NonNull IAnalysisProgressListener listener) {
    }

    @Override
    public @NonNull Iterable<@NonNull ISegmentAspect> getSegmentAspects() {
        return Collections.emptyList();
    }

    @Override
    public @Nullable ISegmentStore<@NonNull ISegment> getSegmentStore() {
        return fCss;
    }

}
