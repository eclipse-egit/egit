/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * A {@link TitleAreaDialog} with a default image.
 */
public class TitleAndImageDialog extends TitleAreaDialog {

	private ResourceManager resourceManager;

	private ImageDescriptor imageDescriptor;

	private boolean explicitImageSet;

	/**
	 * Creates a new {@link TitleAndImageDialog} with the given default image.
	 *
	 * @param parent
	 *            the parent SWT {@link Shell}
	 * @param imageDescriptor
	 *            to use for the default image used if no image is explicitly
	 *            set
	 */
	public TitleAndImageDialog(Shell parent, ImageDescriptor imageDescriptor) {
		super(parent);
		this.imageDescriptor = imageDescriptor;
	}

	@Override
	protected Control createContents(Composite parent) {
		if (imageDescriptor != null && !explicitImageSet) {
			super.setTitleImage(
					getResourceManager().createImage(imageDescriptor));
		}
		Control control = super.createContents(parent);
		control.addDisposeListener(event -> {
			if (resourceManager != null) {
				resourceManager.dispose();
				resourceManager = null;
			}
		});
		return control;
	}

	@Override
	public void setTitleImage(Image newTitleImage) {
		explicitImageSet = newTitleImage != null;
		super.setTitleImage(newTitleImage);
	}

	/**
	 * Retrieves this dialog's {@link LocalResourceManager}.
	 *
	 * @return the {@link LocalResourceManager}
	 */
	protected @NonNull ResourceManager getResourceManager() {
		if (resourceManager == null) {
			resourceManager = new LocalResourceManager(
					JFaceResources.getResources());
		}
		return resourceManager;
	}
}
