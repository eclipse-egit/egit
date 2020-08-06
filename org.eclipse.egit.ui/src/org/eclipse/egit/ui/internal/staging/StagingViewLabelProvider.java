/*******************************************************************************
 * Copyright (C) 2011, 2020 Bernard Leach <leachbj@bouncycastle.org> and others.
 * Copyright (C) 2015 Denis Zygann <d.zygann@web.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.staging.StagingView.Presentation;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Label provider for {@link StagingEntry} objects
 */
public class StagingViewLabelProvider extends LabelProvider {

	private StagingView stagingView;

	private WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();

	private final Image FOLDER = PlatformUI.getWorkbench().getSharedImages()
			.getImage(ISharedImages.IMG_OBJ_FOLDER);

	private ResourceManager resourceManager = new LocalResourceManager(
			JFaceResources.getResources());

	private boolean fileNameMode = false;

	/**
	 * @param stagingView
	 */
	public StagingViewLabelProvider(StagingView stagingView) {
		super();
		this.stagingView = stagingView;
	}

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

	@Override
	public void dispose() {
		this.resourceManager.dispose();
		super.dispose();
	}

	private Image getEditorImage(StagingEntry diff) {
		if (diff.isSubmodule()) {
			return (Image) resourceManager.get(UIIcons.REPOSITORY);
		}

		Image image;
		if (diff.getPath() != null) {
			image = (Image) resourceManager
					.get(UIUtils.getEditorImage(diff.getPath()));
		} else {
			image = (Image) resourceManager.get(UIUtils.DEFAULT_FILE_IMG);
		}
		if (diff.isSymlink()) {
			if (diff.getLocation().toFile().isDirectory()) {
				image = FOLDER;
			}
			image = addSymlinkDecorationToImage(image);
		}
		return image;
	}

	private Image addSymlinkDecorationToImage(Image base) {
		DecorationOverlayIcon decorated = new DecorationOverlayIcon(base,
				UIIcons.OVR_SYMLINK, IDecoration.TOP_RIGHT);
		return (Image) this.resourceManager.get(decorated);
	}

	@Override
	public Image getImage(Object element) {

		if (element instanceof StagingFolderEntry) {
			StagingFolderEntry c = (StagingFolderEntry) element;
			if (c.getContainer() == null) {
				return FOLDER;
			}
			return workbenchLabelProvider
					.getImage(((StagingFolderEntry) element).getContainer());
		}

		StagingEntry c = (StagingEntry) element;
		return getEditorImage(c);
	}

	@Override
	public String getText(Object element) {

		if (element instanceof StagingFolderEntry) {
			StagingFolderEntry stagingFolderEntry = (StagingFolderEntry) element;
			return stagingFolderEntry.getNodePath().toString();
		}

		StagingEntry stagingEntry = getStagingEntry(element);

		if (stagingEntry == null) {
			return ""; //$NON-NLS-1$
		}

		if (stagingView.getPresentation() == Presentation.LIST) {
			if (fileNameMode) {
				IPath parsed = Path.fromOSString(stagingEntry.getPath());
				if (parsed.segmentCount() > 1) {
					return parsed.lastSegment() + " - " //$NON-NLS-1$
							+ parsed.removeLastSegments(1).toString();
				}
			}
			return stagingEntry.getPath();
		}
		return stagingEntry.getName();
	}

	@Nullable
	private StagingEntry getStagingEntry(Object element) {
		StagingEntry entry = null;

		if (element instanceof StagingEntry) {
			entry = (StagingEntry) element;
		}

		if (element instanceof TreeItem) {
			TreeItem item = (TreeItem) element;
			if (item.getData() instanceof StagingEntry) {
				entry = (StagingEntry) item.getData();
			}
		}
		return entry;
	}

}
