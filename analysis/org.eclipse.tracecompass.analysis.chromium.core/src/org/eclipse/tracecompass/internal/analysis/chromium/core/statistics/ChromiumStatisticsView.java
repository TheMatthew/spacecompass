package org.eclipse.tracecompass.internal.analysis.chromium.core.statistics;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.statistics.AbstractSegmentStoreStatisticsView;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.statistics.AbstractSegmentStoreStatisticsViewer;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;

public class ChromiumStatisticsView extends AbstractSegmentStoreStatisticsView {

    public static final String ID = "org.eclipse.tracecompass.internal.analysis.chromium.core.statistics.view";

    public ChromiumStatisticsView() {
    }

    @Override
    protected @NonNull AbstractSegmentStoreStatisticsViewer createSegmentStoreStatisticsViewer(@NonNull Composite parent) {
        return new AbstractSegmentStoreStatisticsViewer(parent) {
            @Override
            protected @Nullable TmfAbstractAnalysisModule createStatisticsAnalysiModule() {
                return new ChromiumStatisticsAnalysis();
            }
        };
    }

}
