/*******************************************************************************
 * Copyright (C) 2011, 2014 Bernard Leach <leachbj@bouncycastle.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.decorators.DecorationResult;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator.DecorationHelper;
import org.eclipse.egit.ui.internal.staging.StagingView.Presentation;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.util.FS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Label provider for {@link StagingEntry} objects
 */
public class StagingViewLabelProvider extends LabelProvider {

	private StagingView stagingView;

	private WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();

	private Image DEFAULT = PlatformUI.getWorkbench().getSharedImages()
			.getImage(ISharedImages.IMG_OBJ_FILE);

	private final Image FOLDER = PlatformUI.getWorkbench().getSharedImages()
			.getImage(ISharedImages.IMG_OBJ_FOLDER);

	private final Image SUBMODULE = UIIcons.REPOSITORY.createImage();

	private ResourceManager resourceManager = new LocalResourceManager(
			JFaceResources.getResources());

	private final DecorationHelper decorationHelper = new DecorationHelper(
			Activator.getDefault().getPreferenceStore());


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
		SUBMODULE.dispose();
		this.resourceManager.dispose();
		super.dispose();
	}

	private Image getEditorImage(StagingEntry diff) {
		if (diff.isSubmodule())
			return SUBMODULE;

		Image image = DEFAULT;
		String name = new Path(diff.getPath()).lastSegment();
		if (name != null) {
			ImageDescriptor descriptor = PlatformUI.getWorkbench()
					.getEditorRegistry().getImageDescriptor(name);
			image = (Image) this.resourceManager.get(descriptor);
		}
		if (diff.isSymlink()) {
			try {
				IPath diffLocation = diff.getLocation();
				if (diffLocation != null) {
					File diffFile = diffLocation.toFile();
					if (diffFile.exists()) {
						String targetPath = FS.DETECTED.readSymLink(diffFile);
						if (targetPath != null
								&& new File(diffFile, targetPath).isDirectory())
							image = FOLDER;
					}
				}
			} catch (IOException e) {
				Activator
						.error(UIText.StagingViewLabelProvider_SymlinkError, e);
			}
			image = addSymlinkDecorationToImage(image);
		}
		return image;
	}

	private Image getDecoratedImage(Image base, ImageDescriptor decorator) {
		DecorationOverlayIcon decorated = new DecorationOverlayIcon(base,
				decorator, IDecoration.BOTTOM_RIGHT);
		return (Image) this.resourceManager.get(decorated);
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
		DecorationResult decoration = new DecorationResult();
		decorationHelper.decorate(decoration, c);
		return getDecoratedImage(getEditorImage(c), decoration.getOverlay());
	}

	@Override
	public String getText(Object element) {

		if (element instanceof StagingFolderEntry) {
			StagingFolderEntry stagingFolderEntry = (StagingFolderEntry) element;
			return stagingFolderEntry.getNodePath().toString();
		}

		StagingEntry stagingEntry = (StagingEntry) element;
		final DecorationResult decoration = new DecorationResult();
		decorationHelper.decorate(decoration, stagingEntry);
		final StyledString styled = new StyledString();
		final String prefix = decoration.getPrefix();
		final String suffix = decoration.getSuffix();
		if (prefix != null)
			styled.append(prefix, StyledString.DECORATIONS_STYLER);
		if (stagingView.getPresentation() == Presentation.LIST) {
			if (fileNameMode) {
				IPath parsed = Path.fromOSString(stagingEntry.getPath());
				if (parsed.segmentCount() > 1) {
					styled.append(parsed.lastSegment());
					if (suffix != null)
						styled.append(suffix, StyledString.DECORATIONS_STYLER);
					styled.append(' ');
					styled.append('-', StyledString.QUALIFIER_STYLER);
					styled.append(' ');
					styled.append(parsed.removeLastSegments(1).toString(),
							StyledString.QUALIFIER_STYLER);
				} else {
					styled.append(stagingEntry.getPath());
					if (suffix != null)
						styled.append(suffix, StyledString.DECORATIONS_STYLER);
				}
			} else {
				styled.append(stagingEntry.getPath());
				if (suffix != null)
					styled.append(suffix, StyledString.DECORATIONS_STYLER);
			}
		} else {
			styled.append(stagingEntry.getName());
		}
		return styled.toString();
	}

}
