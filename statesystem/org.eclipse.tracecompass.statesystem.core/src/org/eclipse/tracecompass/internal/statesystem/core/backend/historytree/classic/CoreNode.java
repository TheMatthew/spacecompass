/*******************************************************************************
 * Copyright (c) 2010, 2016 Ericsson, École Polytechnique de Montréal, and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 *   Florian Wininger - Add Extension and Leaf Node
 *******************************************************************************/

package org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.classic;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.HTConfig;
import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.HTNode;
import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.ParentNode;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;

/**
 * A Core node is a first-level node of a History Tree which is not a leaf node.
 *
 * It extends HTNode by adding support for child nodes, and also extensions.
 *
 * @author Alexandre Montplaisir
 */
public final class CoreNode extends ParentNode {

    /** Nb. of children this node has */
    private int nbChildren;

    /** Seq. numbers of the children nodes (size = MAX_NB_CHILDREN) */
    private int[] children;

    /** Start times of each of the children (size = MAX_NB_CHILDREN) */
    private long[] childStart;

    /** Seq number of this node's extension. -1 if none */
    private volatile int extension = -1;

    /**
     * Lock used to gate the accesses to the children arrays. Meant to be a
     * different lock from the one in {@link HTNode}.
     */
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock(false);

    /**
     * Initial constructor. Use this to initialize a new EMPTY node.
     *
     * @param config
     *            Configuration of the History Tree
     * @param seqNumber
     *            The (unique) sequence number assigned to this particular node
     * @param parentSeqNumber
     *            The sequence number of this node's parent node
     * @param start
     *            The earliest timestamp stored in this node
     */
    public CoreNode(HTConfig config, int seqNumber, int parentSeqNumber,
            long start) {
        super(config, seqNumber, parentSeqNumber, start);
        this.nbChildren = 0;
        int size = config.getMaxChildren();

        /*
         * We instantiate the two following arrays at full size right away,
         * since we want to reserve that space in the node's header.
         * "this.nbChildren" will tell us how many relevant entries there are in
         * those tables.
         */
        this.children = new int[size];
        this.childStart = new long[size];
    }

    @Override
    protected void readSpecificHeader(ByteBuffer buffer) {
        int size = getConfig().getMaxChildren();

        extension = buffer.getInt();
        nbChildren = buffer.getInt();

        children = new int[size];
        for (int i = 0; i < nbChildren; i++) {
            children[i] = buffer.getInt();
        }
        for (int i = nbChildren; i < size; i++) {
            buffer.getInt();
        }

        this.childStart = new long[size];
        for (int i = 0; i < nbChildren; i++) {
            childStart[i] = buffer.getLong();
        }
        for (int i = nbChildren; i < size; i++) {
            buffer.getLong();
        }
    }

    @Override
    protected void writeSpecificHeader(ByteBuffer buffer) {
        int size = getConfig().getMaxChildren();

        buffer.putInt(extension);
        buffer.putInt(nbChildren);

        /* Write the "children's seq number" array */
        for (int i = 0; i < nbChildren; i++) {
            buffer.putInt(children[i]);
        }
        for (int i = nbChildren; i < size; i++) {
            buffer.putInt(0);
        }

        /* Write the "children's start times" array */
        for (int i = 0; i < nbChildren; i++) {
            buffer.putLong(childStart[i]);
        }
        for (int i = nbChildren; i < size; i++) {
            buffer.putLong(0);
        }
    }

    @Override
    public int getNbChildren() {
        rwl.readLock().lock();
        try {
            return nbChildren;
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public int getChild(int index) {
        rwl.readLock().lock();
        try {
            return children[index];
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public int getLatestChild() {
        rwl.readLock().lock();
        try {
            return children[nbChildren - 1];
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public long getChildStart(int index) {
        rwl.readLock().lock();
        try {
            return childStart[index];
        } finally {
            rwl.readLock().unlock();
        }
    }

    /**
     * Get the sequence number of the extension to this node (if there is one).
     *
     * @return The sequence number of the extended node. '-1' is returned if
     *         there is no extension node.
     */
    public int getExtensionSequenceNumber() {
        rwl.readLock().lock();
        try {
            return extension;
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public void linkNewChild(HTNode childNode) {
        rwl.writeLock().lock();
        try {
            if (nbChildren >= getConfig().getMaxChildren()) {
                throw new IllegalStateException("Asked to link another child but parent already has maximum number of children"); //$NON-NLS-1$
            }

            children[nbChildren] = childNode.getSequenceNumber();
            childStart[nbChildren] = childNode.getNodeStart();
            nbChildren++;

        } finally {
            rwl.writeLock().unlock();
        }
    }

    @Override
    public Collection<Integer> selectNextChildren(long t) throws TimeRangeException {
        if (t < getNodeStart() || (isOnDisk() && t > getNodeEnd())) {
            throw new TimeRangeException("Requesting children outside the node's range: " + t); //$NON-NLS-1$
        }
        rwl.readLock().lock();
        try {
            int potentialNextSeqNb = -1;
            for (int i = 0; i < nbChildren; i++) {
                if (t >= childStart[i]) {
                    potentialNextSeqNb = children[i];
                } else {
                    break;
                }
            }

            if (potentialNextSeqNb == -1) {
                throw new IllegalStateException("No next child node found"); //$NON-NLS-1$
            }
            return Collections.singleton(potentialNextSeqNb);
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CORE;
    }

    @Override
    protected int getSpecificHeaderSize() {
        int maxChildren = getConfig().getMaxChildren();
        int specificSize =
                  Integer.BYTES /* 1x int (extension node) */
                + Integer.BYTES /* 1x int (nbChildren) */

                /* MAX_NB * int ('children' table) */
                + Integer.BYTES * maxChildren

                /* MAX_NB * Timevalue ('childStart' table) */
                + Long.BYTES * maxChildren;

        return specificSize;
    }

    @Override
    public String toStringSpecific() {
        /* Only used for debugging, shouldn't be externalized */
        return String.format("Core Node, %d children %s", //$NON-NLS-1$
                nbChildren, Arrays.toString(Arrays.copyOf(children, nbChildren)));
    }

}
