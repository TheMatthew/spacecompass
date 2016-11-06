package org.eclipse.tracecompass.analysis.chromium.core.trace;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

final class SortingJob extends Job {
    private static final int CHUNK_SIZE = 65535;

    private static final class Pair implements Comparable<Pair> {
        public Pair(String string, int i) {
            line = string;
            String key = "\"ts\":";
            int index = string.indexOf(key) + key.length();
            int end = string.indexOf(',', index);
            String number = string.substring(index, end);
            ts = 0;
            for (char s : number.toCharArray()) {
                if (s == '.') {
                    continue;
                }
                if (s < '0' || s > '9') {
                    throw new IllegalStateException("invalid string " + number); //$NON-NLS-1$
                }
                ts = (ts * 10) + (s - '0');
            }
            pos = i;
        }

        long ts;
        String line;
        int pos;

        @Override
        public int compareTo(Pair o) {
            return Long.compare(ts, o.ts);
        }
    }

    private final String fPath;
    private final ITmfTrace fTrace;

    public SortingJob(ITmfTrace trace, String path) {
        super("Sorting Trace...");
        fTrace = trace;
        fPath = path;

    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        String dir = TmfTraceManager.getSupplementaryFileDir(fTrace);

        monitor.beginTask("Sorting", IProgressMonitor.UNKNOWN);
        monitor.subTask("Splitting up trace into segments");
        File tempDir = new File(dir + ".tmp"); //$NON-NLS-1$
        tempDir.mkdirs();
        List<File> tracelings = new ArrayList<>();
        try (BufferedInputStream parser = new BufferedInputStream(new FileInputStream(fPath))) {
            char data = (char) parser.read();
            while (data != '{') {
                data = (char) parser.read();
            }
            List<Pair> events = new ArrayList<>(CHUNK_SIZE);
            Pair line = readNextEvent(parser, 0);
            if (line == null) {
                return new Status(IStatus.ERROR, "trace", "null");
            }
            line.line = data + "\"" + line.line;
            int cnt = 0;
            int filen = 0;
            while (line != null) {
                while (cnt < CHUNK_SIZE) {
                    events.add(line);
                    monitor.worked(1);
                    if (monitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                    line = readNextEvent(parser, 0);
                    if (line == null) {
                        break;
                    }
                    cnt++;
                }
                events.sort((o1, o2) -> Long.compare(o1.ts, o2.ts));
                cnt = 0;
                File traceling = new File(tempDir + File.separator + "test" + filen + ".json"); //$NON-NLS-1$ //$NON-NLS-2$
                tracelings.add(traceling);
                traceling.createNewFile();
                try (PrintWriter fs = new PrintWriter(traceling)) {
                    fs.println('[');
                    for (Pair sortedEvent : events) {
                        fs.println(sortedEvent.line + ',');
                    }
                    fs.println(']');
                }
                events.clear();
                filen++;
                monitor.worked(1);
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }

            }
            monitor.subTask("Merging trace segments");
            PriorityQueue<Pair> evs = new PriorityQueue<>();
            List<BufferedInputStream> parsers = new ArrayList<>();
            int i = 0;
            for (File traceling : tracelings) {
                BufferedInputStream createParser = new BufferedInputStream(new FileInputStream(traceling));
                while (data != '{') {
                    data = (char) parser.read();
                }
                Pair parse = readNextEvent(createParser, i);
                evs.add(parse);
                i++;
                parsers.add(createParser);
                monitor.worked(1);
                if (monitor.isCanceled()) {
                    break;
                }
            }
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
            File file = new File(dir + File.separator + new File(fTrace.getPath()).getName());
            file.createNewFile();

            try (PrintWriter tempWriter = new PrintWriter(file)) {
                tempWriter.println('[');
                while (!evs.isEmpty()) {
                    Pair sortedEvent = evs.poll();
                    Pair parse = readNextEvent(parsers.get(sortedEvent.pos), sortedEvent.pos);
                    if (parse != null) {
                        tempWriter.println(sortedEvent.line.trim() + ",");
                        evs.add(parse);
                    } else {
                        tempWriter.println(sortedEvent.line.trim() + (evs.isEmpty() ? "" : ","));
                    }
                    monitor.worked(1);
                    if (monitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                }
                tempWriter.println(']');
            }
            for (BufferedInputStream tmpParser : parsers) {
                tmpParser.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
        } finally {
            for (File tl : tracelings) {
                tl.delete();
            }
            tempDir.delete();

            monitor.done();
        }
        return Status.OK_STATUS;

    }

    private static @Nullable Pair readNextEvent(BufferedInputStream parser, int i) throws IOException {
        StringBuffer sb = new StringBuffer();
        char elem = (char) parser.read();
        if (elem == ']') {
            return null;
        }
        while (elem != '}' && elem != ']' && elem != (char) -1) {
            elem = (char) parser.read();
            sb.append(elem);
        }
        return (elem == '}') ? new Pair(sb.toString(), i) : null;
    }
}