/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

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
 * Label provider for {@link StagingEntry} objects
 */
public class StagingViewLabelProvider extends BaseLabelProvider implements
		ITableLabelProvider {

	private Image DEFAULT = PlatformUI.getWorkbench().getSharedImages()
			.getImage(ISharedImages.IMG_OBJ_FILE);

	private ResourceManager resourceManager = new LocalResourceManager(
			JFaceResources.getResources());

	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0) {
			final StagingEntry c = (StagingEntry) element;
			switch (c.getState()) {
			case ADDED:
				return getDecoratedImage(getEditorImage(c),
						UIIcons.OVR_STAGED_ADD);
			case CHANGED:
				return getDecoratedImage(getEditorImage(c), UIIcons.OVR_DIRTY);
			case REMOVED:
				return getDecoratedImage(getEditorImage(c),
						UIIcons.OVR_STAGED_REMOVE);
			case MISSING:
				return getDecoratedImage(getEditorImage(c),
						UIIcons.OVR_STAGED_REMOVE);
			case MODIFIED:
				return getEditorImage(c);
			case PARTIALLY_MODIFIED:
				return getDecoratedImage(getEditorImage(c), UIIcons.OVR_DIRTY);
			case CONFLICTING:
				return getDecoratedImage(getEditorImage(c), UIIcons.OVR_CONFLICT);
			case UNTRACKED:
				return getDecoratedImage(getEditorImage(c), UIIcons.OVR_UNTRACKED);
			default:
				return getEditorImage(c);
			}
		}
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		if (columnIndex == 0) {
			final StagingEntry c = (StagingEntry) element;
			if (c.getState() == StagingEntry.State.MODIFIED || c.getState() == StagingEntry.State.PARTIALLY_MODIFIED)
				return "> " + c.getPath(); //$NON-NLS-1$
			else
				return c.getPath();
		}
		return null;
	}

	@Override
	public void dispose() {
		this.resourceManager.dispose();
		super.dispose();
	}

	private Image getEditorImage(StagingEntry diff) {
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
}
