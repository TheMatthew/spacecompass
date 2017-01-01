/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.chromium.core.event;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;

/**
 * A trace compass log aspect
 *
 * @author Matthew Khouzam
 *
 * @param <T>
 */
interface ITraceEventAspect<T> extends ITmfEventAspect<T> {

    @Override
    default @Nullable T resolve(@NonNull ITmfEvent event) {
        if (event instanceof TraceEventEvent) {
            return resolveTCL((org.eclipse.tracecompass.internal.analysis.chromium.core.event.TraceEventEvent) event);
        }
        return null;
    }

    T resolveTCL(@NonNull TraceEventEvent event);

}
