/*******************************************************************************
 * Copyright (c) 2013, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.tmf.core.callstack;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * The base classes for analyses who want to populate the CallStack state
 * system.
 *
 * @author Matthew Khouzam
 * @since 2.1
 */
public abstract class CallStackAnalysis extends TmfStateSystemAnalysisModule {

    private static final String LINKS_FILE = CallStackAnalysis.class.getCanonicalName() + ".links.dat"; //$NON-NLS-1$

    private static final Logger LOGGER = Logger.getLogger(CallStackAnalysis.class.getCanonicalName());

    private static final String[] DEFAULT_PROCESSES_PATTERN = new String[] { CallStackStateProvider.PROCESSES, "*" }; //$NON-NLS-1$
    private static final String[] DEFAULT_THREADS_PATTERN = new String[] { "*" }; //$NON-NLS-1$
    private static final String[] DEFAULT_CALL_STACK_PATH = new String[] { CallStackStateProvider.CALL_STACK };

    private Collection<TimeGraphVertex> fLinks = Collections.emptyList();

    /**
     * Abstract constructor (should only be called via the sub-classes'
     * constructors.
     */
    protected CallStackAnalysis() {
        super();
    }

    /**
     * The quark pattern, relative to the root, to get the list of attributes
     * representing the different processes of a trace.
     * <p>
     * If the trace does not define processes, an empty array can be returned.
     * <p>
     * The pattern is passed as-is to
     * {@link org.eclipse.tracecompass.statesystem.core.ITmfStateSystem#getQuarks(String...)}.
     * <p>
     * Override this method if the state system attributes do not match the
     * default pattern defined by {@link CallStackStateProvider}.
     *
     * @return The quark pattern to find the process attributes
     */
    public String[] getProcessesPattern() {
        return DEFAULT_PROCESSES_PATTERN;
    }

    /**
     * The quark pattern, relative to an attribute found by
     * {@link #getProcessesPattern()}, to get the list of attributes
     * representing the threads of a process, or the threads a trace if the
     * process pattern was empty.
     * <p>
     * If the trace does not define threads, an empty array can be returned.
     * <p>
     * This will be passed as-is to
     * {@link org.eclipse.tracecompass.statesystem.core.ITmfStateSystem#getQuarks(int, String...)}.
     * <p>
     * Override this method if the state system attributes do not match the
     * default pattern defined by {@link CallStackStateProvider}.
     *
     * @return The quark pattern to find the thread attributes
     */
    public String[] getThreadsPattern() {
        return DEFAULT_THREADS_PATTERN;
    }

    /**
     * Get the call stack attribute path, relative to an attribute found by the
     * combination of {@link #getProcessesPattern()} and
     * {@link #getThreadsPattern()}.
     * <p>
     * Override this method if the state system attributes do not match the
     * default pattern defined by {@link CallStackStateProvider}.
     *
     * @return the relative path of the call stack attribute
     */
    public String[] getCallStackPath() {
        return DEFAULT_CALL_STACK_PATH;
    }

    /**
     * Get the links in a callstack
     *
     * @return the collection of the callstack links
     * @since 2.3
     */
    public Collection<TimeGraphVertex> getCallstackLinks() {
        return fLinks;
    }

    @Override
    protected boolean executeAnalysis(@Nullable IProgressMonitor monitor) {
        ITmfTrace trace = getTrace();
        // TODO make links into a segment store
        if (trace == null) {
            return false;
        }
        String file = TmfTraceManager.getSupplementaryFileDir(trace);
        File linksFile = new File(file + File.separator + LINKS_FILE);
        if (linksFile.exists()) {
            try (FileChannel fc = FileChannel.open(linksFile.toPath(), StandardOpenOption.READ)) {
                ByteBuffer bb = fc.map(MapMode.READ_ONLY, 0, fc.size());
                ImmutableList.Builder<TimeGraphVertex> builder = new Builder<>();
                while (bb.hasRemaining()) {
                    TimeGraphVertex create = TimeGraphVertex.create(bb);
                    if (create == null) {
                        break;
                    }
                    builder.add(create);
                }
                fLinks = builder.build();
            } catch (IOException e) {
                LOGGER.config("Cannot load links, rebuilding them " + linksFile.getAbsolutePath() + " cause " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        boolean ret = super.executeAnalysis(monitor);
        if (ret) {
            if (!linksFile.exists()) {
                try {
                    linksFile.createNewFile();
                    try (FileChannel fc = FileChannel.open(linksFile.toPath(), StandardOpenOption.APPEND)) {
                        ByteBuffer bb = ByteBuffer.allocate(fLinks.size() * TimeGraphVertex.SIZE);
                        for (TimeGraphVertex link : fLinks) {
                            bb.put(link.serialize());
                        }
                        bb.flip();
                        fc.write(bb, 0);

                    }
                } catch (IOException e) {
                    LOGGER.config("Cannot load links, rebuilding them " + linksFile.getAbsolutePath() + " cause " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }
        return ret;
    }

    /**
     * Set the link list
     *
     * @param links
     *            the links to add
     * @since 2.3
     */
    protected void setLinks(Collection<TimeGraphVertex> links) {
        fLinks = links;
    }

}
