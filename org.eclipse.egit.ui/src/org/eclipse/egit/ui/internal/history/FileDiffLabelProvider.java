/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Label provider for {@link FileDiff} objects
 */
public class FileDiffLabelProvider extends BaseLabelProvider implements
		ITableLabelProvider {

	private Image DEFAULT = PlatformUI.getWorkbench().getSharedImages()
			.getImage(ISharedImages.IMG_OBJ_FILE);

	private ResourceManager resourceManager = new LocalResourceManager(
			JFaceResources.getResources());

	public String getColumnText(final Object element, final int columnIndex) {
		if (columnIndex == 0) {
			final FileDiff c = (FileDiff) element;
			return c.getPath();
		}
		return null;
	}

	private Image getEditorImage(FileDiff diff) {
		Image image = DEFAULT;
		String name = new Path(diff.getPath()).lastSegment();
		if (name != null) {
			ImageDescriptor descriptor = PlatformUI.getWorkbench()
					.getEditorRegistry().getImageDescriptor(name);
			image = (Image) this.resourceManager.get(descriptor);
		}
		return image;
	}

	private Image getDecoratedImage(Image base, ImageDescriptor decorator) {
		DecorationOverlayIcon decorated = new DecorationOverlayIcon(base,
				decorator, IDecoration.BOTTOM_RIGHT);
		return (Image) this.resourceManager.get(decorated);
	}

	public Image getColumnImage(final Object element, final int columnIndex) {
		if (columnIndex == 0) {
			final FileDiff c = (FileDiff) element;
			switch (c.getChange()) {
			case ADD:
				return getDecoratedImage(getEditorImage(c),
						UIIcons.OVR_STAGED_ADD);
			case DELETE:
				return getDecoratedImage(getEditorImage(c),
						UIIcons.OVR_STAGED_REMOVE);
			case COPY:
				// fall through
			case RENAME:
				// fall through
			case MODIFY:
				return getEditorImage(c);
			}
		}
		return null;
	}

	@Override
	public void dispose() {
		this.resourceManager.dispose();
		super.dispose();
	}

}
