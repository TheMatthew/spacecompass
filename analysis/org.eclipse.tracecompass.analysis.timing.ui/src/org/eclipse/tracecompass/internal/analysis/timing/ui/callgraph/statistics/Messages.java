/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Bernd Hufmann - Initial API and implementation
 *******************************************************************************/
package org.eclipse.tracecompass.internal.analysis.timing.ui.callgraph.statistics;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.osgi.util.NLS;

/**
 * Messages used in the Callgraph Statitics.
 *
 * @author Matthew Khouzam
 */
@NonNullByDefault({})
public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.internal.analysis.timing.ui.callgraph.statistics.messages"; //$NON-NLS-1$
    /** Name of the Call graph statistics level in statistics tree */
    public static String CallgraphStatistics_FunctionName;
    /** Name of Total statistics */
    public static String CallgraphStatistics_TotalLabel;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
