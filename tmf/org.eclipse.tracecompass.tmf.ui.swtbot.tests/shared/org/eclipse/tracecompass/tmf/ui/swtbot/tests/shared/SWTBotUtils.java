/*******************************************************************************
 * Copyright (c) 2014, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.bindings.keys.IKeyLookup;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.keyboard.Keyboard;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.utils.MessageFormat;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.eclipse.tracecompass.internal.tmf.ui.project.operations.NewExperimentOperation;
import org.eclipse.tracecompass.tmf.ui.editors.TmfEventsEditor;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfExperimentElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfExperimentFolder;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfOpenTraceHelper;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectRegistry;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTracesFolder;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.ConditionHelpers.ProjectElementHasChild;
import org.eclipse.tracecompass.tmf.ui.tests.shared.WaitUtils;
import org.eclipse.tracecompass.tmf.ui.views.TracingPerspectiveFactory;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.IHandlerService;
import org.hamcrest.Matcher;

/**
 * SWTBot Helper functions
 *
 * @author Matthew Khouzam
 */
@SuppressWarnings("restriction")
public final class SWTBotUtils {

    private static final String WINDOW_MENU = "Window";
    private static final String PREFERENCES_MENU_ITEM = "Preferences";
    private static boolean fPrintedEnvironment = false;
    private static Logger log = Logger.getLogger(SWTBotUtils.class);

    private SWTBotUtils() {

    }

    private static final String TRACING_PERSPECTIVE_ID = TracingPerspectiveFactory.ID;

    /**
     * Waits for all Eclipse jobs to finish. Times out after
     * WaitUtils#MAX_JOBS_WAIT_TIME by default.
     *
     * @throws RuntimeException
     *             once the waiting time passes the default maximum value
     *
     * @deprecated Use {@link WaitUtils#waitForJobs()} instead
     */
    @Deprecated
    public static void waitForJobs() {
        WaitUtils.waitForJobs();
    }

    /**
     * Sleeps current thread for a given time.
     *
     * @param waitTimeMillis
     *            time in milliseconds to wait
     */
    public static void delay(final long waitTimeMillis) {
        try {
            Thread.sleep(waitTimeMillis);
        } catch (final InterruptedException e) {
            // Ignored
        }
    }

    /**
     * Create a tracing project
     *
     * @param projectName
     *            the name of the tracing project
     */
    public static void createProject(final String projectName) {
        /*
         * Make a new test
         */
        UIThreadRunnable.syncExec(new VoidResult() {
            @Override
            public void run() {
                IProject project = TmfProjectRegistry.createProject(projectName, null, new NullProgressMonitor());
                assertNotNull(project);
            }
        });

        WaitUtils.waitForJobs();
    }

    /**
     * Deletes a project
     *
     * @param projectName
     *            the name of the tracing project
     * @param deleteResources
     *            whether or not to deleted resources under the project
     * @param bot
     *            the workbench bot
     */
    public static void deleteProject(final String projectName, boolean deleteResources, SWTWorkbenchBot bot) {
        // Wait for any analysis to complete because it might create
        // supplementary files
        WaitUtils.waitForJobs();
        try {
            ResourcesPlugin.getWorkspace().getRoot().getProject(projectName).refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
        }

        WaitUtils.waitForJobs();

        closeSecondaryShells(bot);
        WaitUtils.waitForJobs();

        if (!ResourcesPlugin.getWorkspace().getRoot().getProject(projectName).exists()) {
            return;
        }

        final SWTBotView projectViewBot = bot.viewById(IPageLayout.ID_PROJECT_EXPLORER);
        projectViewBot.setFocus();

        SWTBotTree treeBot = projectViewBot.bot().tree();
        SWTBotTreeItem treeItem = treeBot.getTreeItem(projectName);
        SWTBotMenu contextMenu = treeItem.contextMenu("Delete");
        contextMenu.click();

        if (deleteResources) {
            bot.shell("Delete Resources").setFocus();
            final SWTBotCheckBox checkBox = bot.checkBox();
            bot.waitUntil(Conditions.widgetIsEnabled(checkBox));
            checkBox.click();
        }

        final SWTBotButton okButton = bot.button("OK");
        bot.waitUntil(Conditions.widgetIsEnabled(okButton));
        okButton.click();

        WaitUtils.waitForJobs();
    }

    /**
     * Deletes a project and its resources
     *
     * @param projectName
     *            the name of the tracing project
     * @param bot
     *            the workbench bot
     */
    public static void deleteProject(String projectName, SWTWorkbenchBot bot) {
        deleteProject(projectName, true, bot);
    }

    /**
     * Creates an experiment
     *
     * @param bot
     *            a given workbench bot
     * @param projectName
     *            the name of the project, creates the project if needed
     * @param expName
     *            the experiment name
     */
    public static void createExperiment(SWTWorkbenchBot bot, String projectName, final @NonNull String expName) {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        TmfProjectElement tmfProject = TmfProjectRegistry.getProject(project, true);
        TmfExperimentFolder expFolder = tmfProject.getExperimentsFolder();
        assertNotNull(expFolder);
        NewExperimentOperation operation = new NewExperimentOperation(expFolder, expName);
        operation.run(new NullProgressMonitor());

        bot.waitUntil(new DefaultCondition() {
            @Override
            public boolean test() throws Exception {
                TmfExperimentElement experiment = expFolder.getExperiment(expName);
                return experiment != null;
            }

            @Override
            public String getFailureMessage() {
                return "Experiment (" + expName + ") couldn't be created";
            }
        });
    }

    /**
     * Focus on the main window
     *
     * @param shellBots
     *            swtbotshells for all the shells
     */
    public static void focusMainWindow(SWTBotShell[] shellBots) {
        SWTBotShell mainShell = getMainShell(shellBots);
        if (mainShell != null) {
            mainShell.activate();
        }
    }

    private static SWTBotShell getMainShell(SWTBotShell[] shellBots) {
        SWTBotShell mainShell = null;
        for (SWTBotShell shellBot : shellBots) {
            if (shellBot.getText().toLowerCase().contains("eclipse")) {
                mainShell = shellBot;
            }
        }
        return mainShell;
    }

    /**
     * Close all non-main shells that are visible.
     *
     * @param bot
     *            the workbench bot
     */
    public static void closeSecondaryShells(SWTWorkbenchBot bot) {
        SWTBotShell[] shells = bot.shells();
        SWTBotShell mainShell = getMainShell(shells);
        if (mainShell == null) {
            return;
        }

        // Close all non-main shell but make sure we don't close an invisible
        // shell such the special "limbo shell" that Eclipse needs to work
        Arrays.stream(shells)
                .filter(shell -> shell != mainShell)
                .filter(s -> !s.widget.isDisposed())
                .filter(SWTBotShell::isVisible)
                .peek(shell -> log.debug(MessageFormat.format("Closing lingering shell with title {0}", shell.getText())))
                .forEach(SWTBotShell::close);
    }

    /**
     * Close a view with a title
     *
     * @param title
     *            the title, like "welcome"
     * @param bot
     *            the workbench bot
     */
    public static void closeView(String title, SWTWorkbenchBot bot) {
        final List<SWTBotView> openViews = bot.views();
        for (SWTBotView view : openViews) {
            if (view.getTitle().equalsIgnoreCase(title)) {
                view.close();
                bot.waitUntil(ConditionHelpers.ViewIsClosed(view));
            }
        }
    }

    /**
     * Close a view with an id
     *
     * @param viewId
     *            the view id, like
     *            "org.eclipse.linuxtools.tmf.ui.views.histogram"
     * @param bot
     *            the workbench bot
     */
    public static void closeViewById(String viewId, SWTWorkbenchBot bot) {
        final SWTBotView view = bot.viewById(viewId);
        view.close();
        bot.waitUntil(ConditionHelpers.ViewIsClosed(view));
    }

    /**
     * Switch to the tracing perspective
     */
    public static void switchToTracingPerspective() {
        switchToPerspective(TRACING_PERSPECTIVE_ID);
    }

    /**
     * Switch to a given perspective
     *
     * @param id
     *            the perspective id (like
     *            "org.eclipse.linuxtools.tmf.ui.perspective"
     */
    public static void switchToPerspective(final String id) {
        UIThreadRunnable.syncExec(new VoidResult() {
            @Override
            public void run() {
                try {
                    PlatformUI.getWorkbench().showPerspective(id, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
                } catch (WorkbenchException e) {
                    fail(e.getMessage());
                }
            }
        });
    }

    /**
     * Initialize the environment for SWTBot
     */
    public static void initialize() {
        failIfUIThread();

        SWTWorkbenchBot bot = new SWTWorkbenchBot();
        UIThreadRunnable.syncExec(() -> {
            printEnvironment();

            // There seems to be problems on some system where the main shell is
            // not in focus initially. This was seen using Xvfb and Xephyr on
            // some occasions.
            focusMainWindow(bot.shells());

            Shell shell = bot.activeShell().widget;

            // Only adjust shell if it appears to be the top-most
            if (shell.getParent() == null) {
                makeShellFullyVisible(shell);
            }
        });
    }

    private static void printEnvironment() {
        if (fPrintedEnvironment) {
            return;
        }

        // Print some information about the environment that could affect test
        // outcome
        Rectangle bounds = Display.getDefault().getBounds();
        System.out.println("Display size: " + bounds.width + "x" + bounds.height);

        String osVersion = System.getProperty("os.version");
        if (osVersion != null) {
            System.out.println("OS version=" + osVersion);
        }
        String gtkVersion = System.getProperty("org.eclipse.swt.internal.gtk.version");
        if (gtkVersion != null) {
            System.out.println("GTK version=" + gtkVersion);
            // Try to print the GTK theme information as behavior can change
            // depending on the theme
            String gtkTheme = System.getProperty("org.eclipse.swt.internal.gtk.theme");
            System.out.println("GTK theme=" + (gtkTheme == null ? "unknown" : gtkTheme));

            String overlayScrollbar = System.getenv("LIBOVERLAY_SCROLLBAR");
            if (overlayScrollbar != null) {
                System.out.println("LIBOVERLAY_SCROLLBAR=" + overlayScrollbar);
            }
            String ubuntuMenuProxy = System.getenv("UBUNTU_MENUPROXY");
            if (ubuntuMenuProxy != null) {
                System.out.println("UBUNTU_MENUPROXY=" + ubuntuMenuProxy);
            }
        }

        System.out.println("Time zone: " + TimeZone.getDefault().getDisplayName());

        fPrintedEnvironment = true;
    }

    /**
     * If the test is running in the UI thread then fail
     */
    private static void failIfUIThread() {
        if (Display.getCurrent() != null && Display.getCurrent().getThread() == Thread.currentThread()) {
            fail("SWTBot test needs to run in a non-UI thread. Make sure that \"Run in UI thread\" is unchecked in your launch configuration or"
                    + " that useUIThread is set to false in the pom.xml");
        }
    }

    /**
     * Try to make the shell fully visible in the display. If the shell cannot
     * fit the display, it will be positioned so that top-left corner is at
     * <code>(0, 0)</code> in display-relative coordinates.
     *
     * @param shell
     *            the shell to make fully visible
     */
    private static void makeShellFullyVisible(Shell shell) {
        Rectangle displayBounds = shell.getDisplay().getBounds();
        Point absCoord = shell.toDisplay(0, 0);
        Point shellSize = shell.getSize();

        Point newLocation = new Point(absCoord.x, absCoord.y);
        newLocation.x = Math.max(0, Math.min(absCoord.x, displayBounds.width - shellSize.x));
        newLocation.y = Math.max(0, Math.min(absCoord.y, displayBounds.height - shellSize.y));
        if (!newLocation.equals(absCoord)) {
            shell.setLocation(newLocation);
        }
    }

    /**
     * Open a trace, this does not perform any validation though
     *
     * @param projectName
     *            The project name
     * @param tracePath
     *            the path of the trace file (absolute or relative)
     * @param traceType
     *            the trace type id (eg: org.eclipse.linuxtools.btf.trace)
     */
    public static void openTrace(final String projectName, final String tracePath, final String traceType) {
        openTrace(projectName, tracePath, traceType, true);
    }

    /**
     * Open a trace, this does not perform any validation though
     *
     * @param projectName
     *            The project name
     * @param tracePath
     *            the path of the trace file (absolute or relative)
     * @param traceType
     *            the trace type id (eg: org.eclipse.linuxtools.btf.trace)
     * @param delay
     *            delay and wait for jobs
     */
    public static void openTrace(final String projectName, final String tracePath, final String traceType, boolean delay) {
        IStatus status = UIThreadRunnable.syncExec(new Result<IStatus>() {
            @Override
            public IStatus run() {
                try {
                    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
                    TmfTraceFolder destinationFolder = TmfProjectRegistry.getProject(project, true).getTracesFolder();
                    return TmfOpenTraceHelper.openTraceFromPath(destinationFolder, tracePath, PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), traceType);
                } catch (CoreException e) {
                    return new Status(IStatus.ERROR, "", e.getMessage(), e);
                }
            }
        });
        if (!status.isOK()) {
            fail(status.getMessage());
        }

        if (delay) {
            delay(1000);
            WaitUtils.waitForJobs();
        }
    }

    /**
     * Finds an editor and sets focus to the editor
     *
     * @param bot
     *            the workbench bot
     * @param editorName
     *            the editor name
     * @return the corresponding SWTBotEditor
     */
    public static SWTBotEditor activateEditor(SWTWorkbenchBot bot, String editorName) {
        Matcher<IEditorReference> matcher = WidgetMatcherFactory.withPartName(editorName);
        final SWTBotEditor editorBot = bot.editor(matcher);
        IEditorPart iep = editorBot.getReference().getEditor(true);
        final TmfEventsEditor tmfEd = (TmfEventsEditor) iep;
        editorBot.show();
        UIThreadRunnable.syncExec(new VoidResult() {
            @Override
            public void run() {
                tmfEd.setFocus();
            }
        });

        WaitUtils.waitForJobs();
        SWTBotUtils.delay(1000);
        assertNotNull(tmfEd);
        return editorBot;
    }

    /**
     * Opens a trace in an editor and get the TmfEventsEditor
     *
     * @param bot
     *            the workbench bot
     * @param projectName
     *            the name of the project that contains the trace
     * @param elementPath
     *            the trace element path (relative to Traces folder)
     * @return TmfEventsEditor the opened editor
     */
    public static TmfEventsEditor openEditor(SWTWorkbenchBot bot, String projectName, IPath elementPath) {
        final SWTBotView projectExplorerView = bot.viewById(IPageLayout.ID_PROJECT_EXPLORER);
        projectExplorerView.setFocus();
        SWTBot projectExplorerBot = projectExplorerView.bot();

        final SWTBotTree tree = projectExplorerBot.tree();
        projectExplorerBot.waitUntil(ConditionHelpers.IsTreeNodeAvailable(projectName, tree));
        final SWTBotTreeItem treeItem = tree.getTreeItem(projectName);
        treeItem.expand();

        SWTBotTreeItem tracesNode = getTraceProjectItem(projectExplorerBot, treeItem, TmfTracesFolder.TRACES_FOLDER_NAME);
        tracesNode.expand();

        SWTBotTreeItem currentItem = tracesNode;
        for (String segment : elementPath.segments()) {
            currentItem = getTraceProjectItem(projectExplorerBot, currentItem, segment);
            currentItem.doubleClick();
        }

        SWTBotEditor editor = bot.editorByTitle(elementPath.toString());
        IEditorPart editorPart = editor.getReference().getEditor(false);
        assertTrue(editorPart instanceof TmfEventsEditor);
        return (TmfEventsEditor) editorPart;
    }

    /**
     * Returns the child tree item of the specified item at the given sub-path.
     * The project element labels may have a count suffix in the format ' [n]'.
     *
     * @param bot
     *            a given workbench bot
     * @param parentItem
     *            the parent tree item
     * @param path
     *            the desired child element sub-path (without suffix)
     * @return the a {@link SWTBotTreeItem} with the specified name
     */
    public static SWTBotTreeItem getTraceProjectItem(SWTBot bot, final SWTBotTreeItem parentItem, final String... path) {
        SWTBotTreeItem item = parentItem;
        for (String name : path) {
            item = getTraceProjectItem(bot, item, name);
        }
        return item;
    }

    /**
     * Returns the child tree item of the specified item with the given name.
     * The project element label may have a count suffix in the format ' [n]'.
     *
     * @param bot
     *            a given workbench bot
     * @param parentItem
     *            the parent tree item
     * @param name
     *            the desired child element name (without suffix)
     * @return the a {@link SWTBotTreeItem} with the specified name
     */
    public static SWTBotTreeItem getTraceProjectItem(SWTBot bot, final SWTBotTreeItem parentItem, final String name) {
        ProjectElementHasChild condition = new ProjectElementHasChild(parentItem, name);
        bot.waitUntil(condition);
        return condition.getItem();
    }

    /**
     * Select the traces folder
     *
     * @param bot
     *            a given workbench bot
     * @param projectName
     *            the name of the project (it needs to exist or else it would
     *            time out)
     * @return a {@link SWTBotTreeItem} of the "Traces" folder
     */
    public static SWTBotTreeItem selectTracesFolder(SWTWorkbenchBot bot, String projectName) {
        SWTBotTreeItem projectTreeItem = selectProject(bot, projectName);
        projectTreeItem.select();
        SWTBotTreeItem tracesFolderItem = getTraceProjectItem(bot, projectTreeItem, TmfTracesFolder.TRACES_FOLDER_NAME);
        tracesFolderItem.select();
        return tracesFolderItem;
    }

    /**
     * Clear the traces folder
     *
     * @param bot
     *            a given workbench bot
     * @param projectName
     *            the name of the project (needs to exist)
     */
    public static void clearTracesFolder(SWTWorkbenchBot bot, String projectName) {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        TmfProjectElement tmfProject = TmfProjectRegistry.getProject(project, false);
        TmfTraceFolder tracesFolder = tmfProject.getTracesFolder();
        try {
            for (TmfTraceElement traceElement : tracesFolder.getTraces()) {
                traceElement.delete(null);
            }

            final IFolder resource = tracesFolder.getResource();
            resource.accept(new IResourceVisitor() {
                @Override
                public boolean visit(IResource visitedResource) throws CoreException {
                    if (visitedResource != resource) {
                        visitedResource.delete(true, null);
                    }
                    return true;
                }
            }, IResource.DEPTH_ONE, 0);
        } catch (CoreException e) {
            fail(e.getMessage());
        }

        bot.waitUntil(new DefaultCondition() {
            private int fTraceNb = 0;

            @Override
            public boolean test() throws Exception {
                List<TmfTraceElement> traces = tracesFolder.getTraces();
                fTraceNb = traces.size();
                return fTraceNb == 0;
            }

            @Override
            public String getFailureMessage() {
                return "Traces Folder not empty (" + fTraceNb + ")";
            }
        });
    }

    /**
     * Clear the trace folder (using the UI)
     *
     * @param bot
     *            a given workbench bot
     * @param projectName
     *            the name of the project (needs to exist)
     */
    public static void clearTracesFolderUI(SWTWorkbenchBot bot, String projectName) {
        SWTBotTreeItem tracesFolder = selectTracesFolder(bot, projectName);
        tracesFolder.contextMenu().menu("Clear").click();
        String CONFIRM_CLEAR_DIALOG_TITLE = "Confirm Clear";
        bot.waitUntil(Conditions.shellIsActive(CONFIRM_CLEAR_DIALOG_TITLE));

        SWTBotShell shell = bot.shell(CONFIRM_CLEAR_DIALOG_TITLE);
        shell.bot().button("Yes").click();
        bot.waitUntil(Conditions.shellCloses(shell));
        bot.waitWhile(ConditionHelpers.treeItemHasChildren(tracesFolder));
    }

    /**
     * Clear the experiment folder
     *
     * @param bot
     *            a given workbench bot
     * @param projectName
     *            the name of the project (needs to exist)
     */
    public static void clearExperimentFolder(SWTWorkbenchBot bot, String projectName) {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        TmfProjectElement tmfProject = TmfProjectRegistry.getProject(project, false);
        TmfExperimentFolder expFolder = tmfProject.getExperimentsFolder();
        expFolder.getExperiments().forEach(experiment -> {
            IResource resource = experiment.getResource();
            try {
                // Close the experiment if open
                experiment.closeEditors();

                IPath path = resource.getLocation();
                if (path != null) {
                    // Delete supplementary files
                    experiment.deleteSupplementaryFolder();
                }
                // Finally, delete the experiment
                resource.delete(true, null);
            } catch (CoreException e) {
                fail(e.getMessage());
            }
        });

        bot.waitUntil(new DefaultCondition() {
            private int fExperimentNb = 0;

            @Override
            public boolean test() throws Exception {
                List<TmfExperimentElement> experiments = expFolder.getExperiments();
                fExperimentNb = experiments.size();
                return fExperimentNb == 0;
            }

            @Override
            public String getFailureMessage() {
                return "Experiment Folder not empty (" + fExperimentNb + ")";
            }
        });
    }

    /**
     * Select the project in Project Explorer
     *
     * @param bot
     *            a given workbench bot
     * @param projectName
     *            the name of the project (it needs to exist or else it would
     *            time out)
     * @return a {@link SWTBotTreeItem} of the project
     */
    public static SWTBotTreeItem selectProject(SWTWorkbenchBot bot, String projectName) {
        SWTBotView projectExplorerBot = bot.viewByTitle("Project Explorer");
        projectExplorerBot.show();
        // FIXME: Bug 496519. Sometimes, the tree becomes disabled for a certain
        // amount of time. This can happen during a long running operation
        // (BusyIndicator.showWhile) which brings up the modal dialog "operation
        // in progress" and this disables all shells
        projectExplorerBot.bot().waitUntil(Conditions.widgetIsEnabled(projectExplorerBot.bot().tree()));
        SWTBotTreeItem treeItem = projectExplorerBot.bot().tree().getTreeItem(projectName);
        treeItem.select();
        return treeItem;
    }

    /**
     * Open a view by id.
     *
     * @param id
     *            view id.
     */
    public static void openView(final String id) {
        openView(id, null);
    }

    /**
     * Open a view by id and secondary id
     *
     * @param id
     *            view id.
     * @param secondaryId
     *            The secondary ID
     */
    public static void openView(final String id, final @Nullable String secondaryId) {
        final PartInitException res[] = new PartInitException[1];
        UIThreadRunnable.syncExec(new VoidResult() {
            @Override
            public void run() {
                try {
                    if (secondaryId == null) {
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(id);
                    } else {
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(id, secondaryId, IWorkbenchPage.VIEW_ACTIVATE);
                    }
                } catch (PartInitException e) {
                    res[0] = e;
                }
            }
        });
        if (res[0] != null) {
            fail(res[0].getMessage());
        }
        WaitUtils.waitForJobs();
    }

    /**
     * Maximize a table
     *
     * @param tableBot
     *            the {@link SWTBotTable} table
     */
    public static void maximizeTable(SWTBotTable tableBot) {
        final AtomicBoolean controlResized = new AtomicBoolean();
        UIThreadRunnable.syncExec(new VoidResult() {
            @Override
            public void run() {
                tableBot.widget.addControlListener(new ControlAdapter() {
                    @Override
                    public void controlResized(ControlEvent e) {
                        tableBot.widget.removeControlListener(this);
                        controlResized.set(true);
                    }
                });
            }
        });
        try {
            tableBot.pressShortcut(KeyStroke.getInstance(IKeyLookup.CTRL_NAME + "+"), KeyStroke.getInstance("M"));
        } catch (ParseException e) {
            fail();
        }
        new SWTBot().waitUntil(new DefaultCondition() {
            @Override
            public boolean test() throws Exception {
                return controlResized.get();
            }

            @Override
            public String getFailureMessage() {
                return "Control was not resized";
            }
        });
    }

    /**
     * Get the bounds of a cell (SWT.Rectangle) for the specified row and column
     * index in a table
     *
     * @param table
     *            the table
     * @param row
     *            the row of the table to look up
     * @param col
     *            the column of the table to look up
     * @return the bounds in display relative coordinates
     */
    public static Rectangle getCellBounds(final Table table, final int row, final int col) {
        return UIThreadRunnable.syncExec(new Result<Rectangle>() {
            @Override
            public Rectangle run() {
                TableItem item = table.getItem(row);
                Rectangle bounds = item.getBounds(col);
                Point p = table.toDisplay(bounds.x, bounds.y);
                Rectangle rect = new Rectangle(p.x, p.y, bounds.width, bounds.height);
                return rect;
            }
        });
    }

    /**
     * Get the tree item from a tree at the specified location
     *
     * @param bot
     *            the SWTBot
     * @param tree
     *            the tree to find the tree item in
     * @param nodeNames
     *            the path to the tree item, in the form of node names (from
     *            parent to child).
     * @return the tree item
     */
    public static SWTBotTreeItem getTreeItem(SWTBot bot, SWTBotTree tree, String... nodeNames) {
        if (nodeNames.length == 0) {
            return null;
        }

        bot.waitUntil(ConditionHelpers.IsTreeNodeAvailable(nodeNames[0], tree));
        SWTBotTreeItem currentNode = tree.getTreeItem(nodeNames[0]);
        return getTreeItem(bot, currentNode, Arrays.copyOfRange(nodeNames, 1, nodeNames.length));
    }

    /**
     * Get the tree item from a parent tree item at the specified location
     *
     * @param bot
     *            the SWTBot
     * @param treeItem
     *            the treeItem to find the tree item under
     * @param nodeNames
     *            the path to the tree item, in the form of node names (from
     *            parent to child).
     * @return the tree item
     */
    public static SWTBotTreeItem getTreeItem(SWTBot bot, SWTBotTreeItem treeItem, String... nodeNames) {
        if (nodeNames.length == 0) {
            return treeItem;
        }

        SWTBotTreeItem currentNode = treeItem;
        for (int i = 0; i < nodeNames.length; i++) {
            bot.waitUntil(ConditionHelpers.treeItemHasChildren(treeItem));
            currentNode.expand();

            String nodeName = nodeNames[i];
            try {
                bot.waitUntil(ConditionHelpers.IsTreeChildNodeAvailable(nodeName, currentNode));
            } catch (TimeoutException e) {
                // FIXME: Sometimes in a JFace TreeViewer, it expands to
                // nothing. Need to find out why.
                currentNode.collapse();
                currentNode.expand();
                bot.waitUntil(ConditionHelpers.IsTreeChildNodeAvailable(nodeName, currentNode));
            }

            SWTBotTreeItem newNode = currentNode.getNode(nodeName);
            currentNode = newNode;
        }

        return currentNode;
    }

    /**
     * Press the keyboard shortcut that goes to the top of a tree widget. The
     * key combination can differ on different platforms.
     *
     * @param keyboard
     *            the keyboard to use
     */
    public static void pressShortcutGoToTreeTop(Keyboard keyboard) {
        if (SWTUtils.isMac()) {
            keyboard.pressShortcut(Keystrokes.ALT, Keystrokes.UP);
        } else {
            keyboard.pressShortcut(Keystrokes.HOME);
        }
    }

    /**
     * Get the active events editor. Note that this will wait until such editor
     * is available.
     *
     * @param workbenchBot
     *            a given workbench bot
     * @return the active events editor
     */
    public static SWTBotEditor activeEventsEditor(final SWTWorkbenchBot workbenchBot) {
        ConditionHelpers.ActiveEventsEditor condition = new ConditionHelpers.ActiveEventsEditor(workbenchBot, null);
        workbenchBot.waitUntil(condition);
        return condition.getActiveEditor();
    }

    /**
     * Get the active events editor. Note that this will wait until such editor
     * is available.
     *
     * @param workbenchBot
     *            a given workbench bot
     * @param editorTitle
     *            the desired editor title. If null, any active events editor
     *            will be considered valid.
     * @return the active events editor
     */
    public static SWTBotEditor activeEventsEditor(final SWTWorkbenchBot workbenchBot, String editorTitle) {
        ConditionHelpers.ActiveEventsEditor condition = new ConditionHelpers.ActiveEventsEditor(workbenchBot, editorTitle);
        workbenchBot.waitUntil(condition);
        return condition.getActiveEditor();
    }

    /**
     * Open the preferences dialog and return the corresponding shell.
     *
     * @param bot
     *            a given workbench bot
     * @return the preferences shell
     */
    public static SWTBotShell openPreferences(SWTBot bot) {
        if (SWTUtils.isMac()) {
            // On Mac, the Preferences menu item is under the application name.
            // For some reason, we can't access the application menu anymore so
            // we use the keyboard shortcut.
            try {
                bot.activeShell().pressShortcut(KeyStroke.getInstance(IKeyLookup.COMMAND_NAME + "+"), KeyStroke.getInstance(","));
            } catch (ParseException e) {
                fail();
            }
        } else {
            bot.menu(WINDOW_MENU).menu(PREFERENCES_MENU_ITEM).click();
        }

        bot.waitUntil(Conditions.shellIsActive(PREFERENCES_MENU_ITEM));
        return bot.activeShell();
    }

    /**
     * Maximize a view by reference. Calling this a second time will "un-maximize" a view.
     * <p>
     * TODO: if this is useful, maybe uplift to SWTViewBot
     *
     * @param view
     *            the view reference
     */
    public static void maximize(@NonNull IViewPart view) {
        assertNotNull(view);
        IWorkbenchPartSite site = view.getSite();
        assertNotNull(site);
        // The annotation is to make the compiler not complain.
        @Nullable
        Object handlerServiceObject = site.getService(IHandlerService.class);
        assertTrue(handlerServiceObject instanceof IHandlerService);
        IHandlerService handlerService = (IHandlerService) handlerServiceObject;
        try {
            handlerService.executeCommand(IWorkbenchCommandConstants.WINDOW_MAXIMIZE_ACTIVE_VIEW_OR_EDITOR, null);
        } catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e) {
            fail(e.getMessage());
        }
    }
}
