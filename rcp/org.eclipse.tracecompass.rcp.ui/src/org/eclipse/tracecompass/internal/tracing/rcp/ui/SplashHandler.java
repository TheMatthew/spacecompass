/**********************************************************************
 * Copyright (c) 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Bernd Hufmann - Initial API and implementation
 **********************************************************************/

package org.eclipse.tracecompass.internal.tracing.rcp.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.internal.tracing.rcp.ui.messages.Messages;
import org.eclipse.ui.branding.IProductConstants;
import org.eclipse.ui.splash.BasicSplashHandler;

/**
 * Custom splash handler
 *
 * @author Bernd Hufmann
 */
public class SplashHandler extends BasicSplashHandler {

    private static final Point VERSION_LOCATION = new Point(10, 280);
    private static final Rectangle PROCESS_BAR_RECTANGLE = new Rectangle(10, 300, 480, 15);
    private static final RGB FOREGROUND_COLOR = new RGB(255, 255, 255);
    private final List<Image> fImageList = new ArrayList<>();

    @Override
    public void init(Shell splash) {
        super.init(splash);

        String progressString = null;

        // Try to get the progress bar and message updater.
        IProduct product = Platform.getProduct();
        if (product != null) {
            progressString = product.getProperty(IProductConstants.STARTUP_PROGRESS_RECT);
        }

        loadSplashExtensions();
        final int index = (int) (System.nanoTime() % 5);
        Rectangle progressRect = StringConverter.asRectangle(progressString, PROCESS_BAR_RECTANGLE);
        setProgressRect(progressRect);

        // Set font color.
        setForeground(FOREGROUND_COLOR);

        // Set the software version.
        getContent().addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                e.gc.drawImage(fImageList.get(index), 0, 0);
                e.gc.setForeground(getForeground());
                e.gc.drawText(
                        NLS.bind(Messages.SplahScreen_VersionString,
                                TracingRcpPlugin.getDefault().getBundle().getVersion().toString()),
                        VERSION_LOCATION.x, VERSION_LOCATION.y, true);
            }
        });
    }

    private void loadSplashExtensions() {
        // Get all splash handler extensions
        IExtension[] extensions = Platform.getExtensionRegistry()
                .getExtensionPoint("org.eclipse.tracecompass.splashs").getExtensions();
        // Process all splash handler extensions
        for (int i = 0; i < extensions.length; i++) {
            processSplashExtension(extensions[i]);
        }
    }

    /**
     * Parse the extension points with the images filename.
     */
    private void processSplashExtension(IExtension extension) {
        // Get all splash handler configuration elements
        IConfigurationElement[] elements = extension.getConfigurationElements();
        // Process all splash handler configuration elements
        for (int j = 0; j < elements.length; j++) {
            processSplashElements(elements[j]);
        }
    }

    /**
     * Create the images defined as extension points
     */
    private void processSplashElements(IConfigurationElement configurationElement) {

        String name = configurationElement.getAttribute("image");
        IContributor contrib = configurationElement.getContributor();
        String path = "../" + contrib.getName()+ "/" + name;
        ImageDescriptor descriptor = TracingRcpPlugin.getImageDescriptor(path);
        if (descriptor != null) {
            Image image = descriptor.createImage();
            if (image != null) {
                fImageList.add(image);
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        // Check to see if any images were defined
        if ((fImageList == null) ||
                fImageList.isEmpty()) {
            return;
        }
        // Dispose of all the images
        Iterator<Image> iterator = fImageList.iterator();
        while (iterator.hasNext()) {
            Image image = iterator.next();
            image.dispose();
        }
    }
}
