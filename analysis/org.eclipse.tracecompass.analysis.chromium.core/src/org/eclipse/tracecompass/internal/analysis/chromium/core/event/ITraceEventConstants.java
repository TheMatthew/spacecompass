/**
 *
 */
package org.eclipse.tracecompass.internal.analysis.chromium.core.event;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Constants for the trace event format
 *
 * @author Matthew Khouzam
 */
@NonNullByDefault
public interface ITraceEventConstants {

    /**
     * Timestamp field name
     */
    String TIMESTAMP = "ts"; //$NON-NLS-1$
    /**
     * Duration field name
     */
    String DURATION = "dur"; //$NON-NLS-1$
    /**
     * Name field name
     */
    String NAME = "name"; //$NON-NLS-1$
    /**
     * TID field name
     */
    String TID = "tid"; //$NON-NLS-1$
    /**
     * PID field name
     */
    String PID = "pid"; //$NON-NLS-1$
    /**
     * Phase field name
     */
    String PHASE = "ph"; //$NON-NLS-1$
    /**
     * Category field name
     */
    String CATEGORY = "cat"; //$NON-NLS-1$
    /**
     * Id field name
     */
    String ID = "id"; //$NON-NLS-1$
    /**
     * Arguments field name
     */
    String ARGS = "args"; //$NON-NLS-1$

}
