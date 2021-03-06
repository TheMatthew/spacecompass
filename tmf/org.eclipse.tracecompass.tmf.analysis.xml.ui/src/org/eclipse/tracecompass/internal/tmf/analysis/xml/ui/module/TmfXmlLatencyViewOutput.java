/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.internal.tmf.analysis.xml.ui.module;

import org.eclipse.tracecompass.tmf.ui.analysis.TmfAnalysisViewOutput;

/**
 * Class overriding the default analysis view output for XML pattern analysis
 * latency views
 *
 * @author Jean-Christian Kouame
 */
public class TmfXmlLatencyViewOutput extends TmfAnalysisViewOutput {

    private String fLabel;

    /**
     * @param viewid
     *            The ID of the view
     * @param label
     *            The label of view
     */
    public TmfXmlLatencyViewOutput(String viewid, String label) {
        super(viewid);
        fLabel = label;
    }

    @Override
    public String getName() {
        return fLabel;
    }
}
