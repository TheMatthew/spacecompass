/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.timing.core.callgraph;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.statistics.AbstractSegmentStatisticsAnalysis;
import org.eclipse.tracecompass.segmentstore.core.ISegment;

/**
 * Call graph statistics analysis used to get statistics on each function type.
 *
 * @author Matthew Khouzam
 */
public abstract class CallGraphStatisticsAnalysis extends AbstractSegmentStatisticsAnalysis {

    @Override
    protected @Nullable String getSegmentType(@NonNull ISegment segment) {
        if (segment instanceof AbstractCalledFunction) {
            AbstractCalledFunction calledFunction = (AbstractCalledFunction) segment;
            return String.valueOf(calledFunction.getSymbol());
        }
        return null;
    }

}
