package org.eclipse.tracecompass.internal.analysis.chromium.core.counters.ui;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.internal.analysis.chromium.core.counters.ChromiumCounterAnalysis;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.linecharts.TmfCommonXLineChartViewer;
import org.eclipse.tracecompass.tmf.ui.views.TmfChartView;
import org.swtchart.Chart;
import org.swtchart.ILineSeries;
import org.swtchart.ILineSeries.PlotSymbolType;
import org.swtchart.ISeries;
import org.swtchart.ISeriesSet;
import org.swtchart.LineStyle;

public class CounterChartView extends TmfChartView {

    public static final String ID = "org.eclipse.tracecompass.internal.analysis.chromium.core.counters.ui.counter"; //$NON-NLS-1$

    private boolean fDelta = false;
    private boolean fLog = false;

    public CounterChartView() {
        super(ID);
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        IMenuManager mm = getViewSite().getActionBars().getMenuManager();
        getViewSite().getActionBars().getToolBarManager().add(new Action("Log", SWT.TOGGLE) {

            @Override
            public void run() {
                fLog = !fLog;
                TmfXYChartViewer chart = getChartViewer();
                if (chart != null) {
                    chart.windowRangeUpdated(null);
                }
            }

            @Override
            public boolean isChecked() {
                return fLog;
            }
        });
        mm.add(new Action("Delta", SWT.RADIO) {
            @Override
            public void run() {
                fDelta = true;
                TmfXYChartViewer chart = getChartViewer();
                if (chart != null) {
                    chart.windowRangeUpdated(null);
                }
            }

            @Override
            public boolean isChecked() {
                return fDelta;
            }
        });
        mm.add(new Action("Cumulative", SWT.RADIO) {
            @Override
            public void run() {
                fDelta = false;
                TmfXYChartViewer chart = getChartViewer();
                if (chart != null) {
                    chart.windowRangeUpdated(null);
                }
            }

            @Override
            public boolean isChecked() {
                return !fDelta;
            }
        });
    }

    @Override
    protected TmfXYChartViewer createChartViewer(Composite parent) {
        return new TmfCommonXLineChartViewer(parent, "Counter view", "", "") { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            @Override
            protected void updateData(long start, long end, int points, IProgressMonitor monitor) {
                int nb = points / 10;
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
                double[] xAxis = getXAxis(start, end, nb);
                setXAxis(xAxis);
                for (Integer value : values) {
                    String fullAttributePath = ss.getFullAttributePath(value);

                    try {
                        double[] yVals = new double[xAxis.length];
                        ITmfStateInterval interval = ss.querySingleState(Math.max(ss.getStartTime(), (long) (start - xAxis[1])), value);
                        for (int i = 0; i < xAxis.length; i++) {
                            double xpos = xAxis[i] + start;
                            if (fDelta) {
                                if (interval.getEndTime() > xpos) {
                                    yVals[i] = 0.0;
                                } else {
                                    double prev = interval.getStateValue().unboxDouble();
                                    interval = ss.querySingleState((long) xpos, value);
                                    yVals[i] = interval.getStateValue().unboxDouble() - prev;
                                }
                            } else {
                                if (interval.getEndTime() > xpos) {
                                    yVals[i] = interval.getStateValue().unboxDouble();
                                } else {
                                    interval = ss.querySingleState((long) xpos, value);
                                    yVals[i] = interval.getStateValue().unboxDouble();
                                }
                            }
                            yVals[i] = Math.max(0.01, yVals[i]);
                        }
                        setSeries(fullAttributePath, yVals);
                    } catch (StateSystemDisposedException e) {
                        e.printStackTrace();
                    }
                }
                Display.getDefault().asyncExec(() -> updateDisplay());
            }

            @Override
            protected void updateDisplay() {
                super.updateDisplay();
                Chart chart = getSwtChart();
                chart.getLegend().setPosition(SWT.LEFT);

                ISeriesSet seriesSet = chart.getSeriesSet();
                for (ISeries serie : seriesSet.getSeries()) {
                    if (serie instanceof ILineSeries) {
                        ILineSeries lineSeries = (ILineSeries) serie;
                        lineSeries.setLineStyle(LineStyle.SOLID);
                        lineSeries.setLineWidth(2);
                        lineSeries.setSymbolType(getSymbol(lineSeries.getId()));
                    }
                }
                chart.getAxisSet().adjustRange();
                chart.getAxisSet().getYAxis(0).adjustRange();
                chart.getAxisSet().getYAxis(0).enableLogScale(fLog);

            }

            private PlotSymbolType getSymbol(String name) {
                PlotSymbolType @NonNull [] vals = PlotSymbolType.values();
                return vals[Math.abs(name.hashCode() % vals.length)];
            }

        };
    }

}
