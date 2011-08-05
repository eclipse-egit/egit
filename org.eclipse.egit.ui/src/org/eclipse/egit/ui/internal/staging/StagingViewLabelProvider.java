/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Label provider for {@link StagingEntry} objects
 */
public class StagingViewLabelProvider extends BaseLabelProvider implements
		ITableLabelProvider, IStyledLabelProvider {

	private Image DEFAULT = PlatformUI.getWorkbench().getSharedImages()
			.getImage(ISharedImages.IMG_OBJ_FILE);

	private ResourceManager resourceManager = new LocalResourceManager(
			JFaceResources.getResources());

	private boolean fileNameMode = false;

	/**
	 * Set file name mode to be enabled or disabled. This mode displays the
	 * names of the file first followed by the path to the folder that the file
	 * is in.
	 *
	 * @param enable
	 * @return this label provider
	 */
	public StagingViewLabelProvider setFileNameMode(boolean enable) {
		fileNameMode = enable;
		return this;
	}

	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0)
			return getImage(element);
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		if (columnIndex == 0)
			return getStyledText(element).toString();
		return ""; //$NON-NLS-1$
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

	public StyledString getStyledText(Object element) {
		StyledString styled = new StyledString();
		final StagingEntry c = (StagingEntry) element;
		if (c.getState() == StagingEntry.State.MODIFIED
				|| c.getState() == StagingEntry.State.PARTIALLY_MODIFIED)
			styled.append('>').append(' ');
		if (fileNameMode) {
			IPath parsed = Path.fromOSString(c.getPath());
			if (parsed.segmentCount() > 1) {
				styled.append(parsed.lastSegment());
				styled.append(' ');
				styled.append('-', StyledString.QUALIFIER_STYLER);
				styled.append(' ');
				styled.append(parsed.removeLastSegments(1).toString(),
						StyledString.QUALIFIER_STYLER);
			} else
				styled.append(c.getPath());
		} else
			styled.append(c.getPath());

		return styled;
	}

	public Image getImage(Object element) {
		final StagingEntry c = (StagingEntry) element;
		switch (c.getState()) {
		case ADDED:
			return getDecoratedImage(getEditorImage(c), UIIcons.OVR_STAGED_ADD);
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
}
