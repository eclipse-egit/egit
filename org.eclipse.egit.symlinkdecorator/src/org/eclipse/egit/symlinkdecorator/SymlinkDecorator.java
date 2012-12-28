/*******************************************************************************
 * Copyright (C) 2012 Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.symlinkdecorator;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.swt.graphics.Image;

/**
 * Decorate symbolic links
 */
public class SymlinkDecorator implements ILightweightLabelDecorator {

	private ImageDescriptor createFromURL;
	private Image createImage;

	/**
	 * Constructor
	 */
	public SymlinkDecorator() {
		try {
			createFromURL = ImageDescriptor.createFromURL(new URL(base(), "ovr/symlink.gif")); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		createImage = createFromURL.createImage();
	}

	public void addListener(ILabelProviderListener listener) {
		// empty
	}

	public void dispose() {
		createImage.dispose();
	}

	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
		// empty
	}

	public void decorate(Object element, IDecoration decoration) {
		if (element instanceof ResourceMapping) {
			element = ((ResourceMapping) element).getModelObject();
		}
		if (element instanceof IResource) {
			IResource resource = (IResource) element;
			ResourceAttributes resourceAttributes = resource.getResourceAttributes();
			if (resourceAttributes != null && resourceAttributes.isSymbolicLink())
				decoration.addOverlay(createFromURL);
		}
	}

	private static URL base() throws MalformedURLException {
		return new URL(Activator.getDefault().getBundle().getEntry("/"), //$NON-NLS-1$
				"icons/"); //$NON-NLS-1$
	}
}
