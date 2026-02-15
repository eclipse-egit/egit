/*******************************************************************************
 * Copyright (C) 2026, Eclipse EGit contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.pullrequest;

import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.pullrequest.PullRequestChangedFile.ChangeType;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Label provider for pull request changed files tree
 */
public class PullRequestChangesLabelProvider extends ColumnLabelProvider {

	private final ResourceManager resourceManager;

	private final WorkbenchLabelProvider workbenchLabelProvider;

	private final Image folderImage;

	// Icon size in pixels (reduced from default 16x16)
	private static final int ICON_SIZE = 12;

	/**
	 * Creates a new label provider
	 */
	public PullRequestChangesLabelProvider() {
		this.resourceManager = new LocalResourceManager(
				JFaceResources.getResources());
		this.workbenchLabelProvider = new WorkbenchLabelProvider();
		this.folderImage = PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJ_FOLDER);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof PullRequestChangedFile) {
			return ((PullRequestChangedFile) element).getName();
		} else if (element instanceof PullRequestFolderEntry) {
			return ((PullRequestFolderEntry) element).getLabel();
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Scale an image to the desired icon size
	 *
	 * @param original the original image
	 * @return scaled image managed by the resource manager
	 */
	private Image scaleImage(Image original) {
		if (original == null) {
			return original;
		}
		ImageData data = original.getImageData();
		if (data.width == ICON_SIZE && data.height == ICON_SIZE) {
			return original; // Already the right size
		}
		ImageData scaled = data.scaledTo(ICON_SIZE, ICON_SIZE);
		ImageDescriptor descriptor = ImageDescriptor.createFromImageData(scaled);
		return (Image) resourceManager.get(descriptor);
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof PullRequestChangedFile) {
			PullRequestChangedFile file = (PullRequestChangedFile) element;

			// Get base file icon from editor registry
			ImageDescriptor baseImageDesc = UIUtils
					.getEditorImage(file.getPath());
			Image baseImage = (Image) resourceManager.get(baseImageDesc);
			baseImage = scaleImage(baseImage);

			// Add change type decoration overlay
			ImageDescriptor overlay = getChangeTypeOverlay(
					file.getChangeType());
			if (overlay != null) {
				ImageDescriptor decorated = new DecorationOverlayIcon(
						baseImage, overlay,
						IDecoration.BOTTOM_RIGHT);
				return (Image) resourceManager.get(decorated);
			}

			return baseImage;

		} else if (element instanceof PullRequestFolderEntry) {
			return scaleImage(folderImage);
		}
		return null;
	}

	private ImageDescriptor getChangeTypeOverlay(ChangeType type) {
		switch (type) {
		case ADDED:
			return UIIcons.OVR_STAGED_ADD;
		case DELETED:
			return UIIcons.OVR_STAGED_REMOVE;
		case RENAMED:
			return UIIcons.OVR_STAGED_RENAME;
		case MODIFIED:
		default:
			return null; // No overlay for modified files
		}
	}

	@Override
	public void dispose() {
		resourceManager.dispose();
		workbenchLabelProvider.dispose();
		super.dispose();
	}
}
