package org.eclipse.tracecompass.internal.analysis.chromium.core.counters.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.internal.analysis.chromium.core.counters.ChromiumCounterAnalysis;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.util.Pair;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.linecharts.TmfCommonXLineChartViewer;
import org.eclipse.tracecompass.tmf.ui.views.TmfChartView;
import org.swtchart.Chart;
import org.swtchart.ILineSeries;
import org.swtchart.ILineSeries.PlotSymbolType;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.ISeriesSet;
import org.swtchart.LineStyle;

public class CounterChartView extends TmfChartView {

    public static final String ID = "org.eclipse.tracecompass.internal.analysis.chromium.core.counters.ui.counter"; //$NON-NLS-1$

    public CounterChartView() {
        super(ID);
    }

    private boolean fDelta = true;

    @Override
    protected TmfXYChartViewer createChartViewer(Composite parent) {
        return new TmfCommonXLineChartViewer(parent, "Counter view", "", "") { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            @Override
            protected void updateData(long start, long end, int points, IProgressMonitor monitor) {
                int nb = points / 5;
                IAnalysisModule am = getTrace().getAnalysisModule(ChromiumCounterAnalysis.ID);
                if (!(am instanceof TmfStateSystemAnalysisModule)) {
                    return;
                }
                TmfStateSystemAnalysisModule module = (TmfStateSystemAnalysisModule) am;
                ITmfStateSystem ss = module.getStateSystem();
                if (ss == null) {
                    return;
                }
                List<Integer> values = ss.getQuarks("*", "*"); //$NON-NLS-1$ //$NON-NLS-2$
                Map<String, Pair<double[], double[]>> ySeries = new LinkedHashMap<>();
                double[] xAxis = getXAxis(start, end, nb);
                for (Integer value : values) {
                    String fullAttributePath = ss.getFullAttributePath(value);
                    try {
                        List<@NonNull ITmfStateInterval> vals = StateSystemUtils.queryHistoryRange(ss, value, start, end, nb, monitor);

                        List<Double> xVals = new ArrayList<>();
                        List<Double> yVals = new ArrayList<>();
                        for (int i = 0; i < nb; i++) {
                            double xpos = xAxis[i];
                            double val = find(xpos, vals);
                            if (val != Double.NaN) {
                                xVals.add(xpos);
                                yVals.add(val);
                            }
                        }
                        if (!xVals.isEmpty()) {
                            double[] x = new double[xVals.size()];
                            double[] y = new double[yVals.size()];
                            x[0] = xVals.get(0);
                            y[0] = yVals.get(0);
                            int skip=0;
                            for (int i = 1; i < xVals.size(); i++) {
                                x[i] = xVals.get(i);
                                y[i] = Math.abs(yVals.get(i) - (fDelta ? yVals.get(i - 1) : 0));
                                if( y[0] == y[i]) {
                                    skip++;
                                }
                            }
                            if( skip < xVals.size()-1) {
                                ySeries.put(fullAttributePath, new Pair<>(x, y));
                            }
                        }
                    } catch (AttributeNotFoundException | StateSystemDisposedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                if (ySeries.isEmpty()) {
                    return;
                }
                Display.getDefault().asyncExec(() -> {
                    Chart chart = getSwtChart();
                    chart.getLegend().setPosition(SWT.LEFT);

                    ISeriesSet seriesSet = chart.getSeriesSet();
                    for (Entry<String, Pair<double[], double[]>> ySerie : ySeries.entrySet()) {
                        String name = ySerie.getKey();
                        ILineSeries serie = (ILineSeries) seriesSet.getSeries(name);
                        if (serie == null) {
                            serie = (ILineSeries) seriesSet.createSeries(SeriesType.LINE, name);
                        }
                        serie.setXSeries(ySerie.getValue().getFirst());
                        serie.setYSeries(ySerie.getValue().getSecond());
                        serie.setLineStyle(LineStyle.SOLID);
                        serie.setLineWidth(2);
                        serie.setSymbolType(getSymbol(name));
                    }
                    chart.getAxisSet().adjustRange();
                });
            }

            private PlotSymbolType getSymbol(String name) {
                PlotSymbolType @NonNull [] vals = PlotSymbolType.values();
                return vals[Math.abs(name.hashCode() % vals.length)];
            }

            private double find(double xPos, List<@NonNull ITmfStateInterval> vals) {
                for (ITmfStateInterval val : vals) {
                    if (val.getStartTime() <= xPos && val.getEndTime() > xPos) {
                        return val.getStateValue().unboxDouble();
                    }
                }
                return 0;
            }

        };
    }

}
