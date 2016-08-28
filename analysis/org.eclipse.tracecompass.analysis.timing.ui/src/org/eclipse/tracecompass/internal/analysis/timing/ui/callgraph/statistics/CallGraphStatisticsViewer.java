/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.internal.analysis.timing.ui.callgraph.statistics;

import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.statistics.AbstractSegmentStoreStatisticsViewer;
import org.eclipse.tracecompass.internal.analysis.timing.ui.callgraph.CallGraphStatisticsAnalysisUI;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.symbols.SymbolProviderManager;

/**
 * A tree viewer implementation for displaying function duration statistics
 *
 * @author Matthew Khouzam
 *
 */
public class CallGraphStatisticsViewer extends AbstractSegmentStoreStatisticsViewer {

    private static final class SymbolFormatter implements Function<SegmentStoreStatisticsEntry, String> {

        private final @Nullable ITmfTrace fTrace;

        public SymbolFormatter(@Nullable ITmfTrace trace) {
            fTrace = trace;
        }

        @Override
        public String apply(@NonNull SegmentStoreStatisticsEntry stat) {
            String original = stat.getName();
            ITmfTrace trace = fTrace;
            if (trace != null) {
                try {
                    Long address = Long.parseLong(original, 10);
                    String res = SymbolProviderManager.getInstance().getSymbolProvider(trace).getSymbolText(address);
                    if (res != null) {
                        return res;
                    }
                    return "0x" + Long.toHexString(address); //$NON-NLS-1$
                } catch (NumberFormatException e) {
                    // was a string after all
                }
            }
            return String.valueOf(original);
        }
    }

    /**
     * Constructor
     *
     * @param parent
     *            the parent composite
     */
    public CallGraphStatisticsViewer(Composite parent) {
        super(parent);
        setLabelProvider(new SegmentStoreStatisticsLabelProvider() {
            @Override
            public @NonNull String getColumnText(@Nullable Object element, int columnIndex) {
                if (columnIndex == 0 && (element instanceof SegmentStoreStatisticsEntry)) {
                    SegmentStoreStatisticsEntry entry = (SegmentStoreStatisticsEntry) element;
                    SymbolFormatter fe = new SymbolFormatter(getTrace());
                    return fe.apply(entry);
                }
                return super.getColumnText(element, columnIndex);
            }
        });
    }

    /**
     * Gets the statistics analysis module
     *
     * @return the statistics analysis module
     */
    @Override
    protected @Nullable TmfAbstractAnalysisModule createStatisticsAnalysiModule() {
        return new CallGraphStatisticsAnalysisUI();
    }
}
