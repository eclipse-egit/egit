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
import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.decorators.DecorationResult;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator.DecorationHelper;
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

	private final Image SUBMODULE = UIIcons.REPOSITORY.createImage();

	private ResourceManager resourceManager = new LocalResourceManager(
			JFaceResources.getResources());

	private final DecorationHelper decorationHelper = new DecorationHelper(
			Activator.getDefault().getPreferenceStore());


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
		return image;
	}

	private Image getDecoratedImage(Image base, ImageDescriptor decorator) {
		DecorationOverlayIcon decorated = new DecorationOverlayIcon(base,
				decorator, IDecoration.BOTTOM_RIGHT);
		return (Image) this.resourceManager.get(decorated);
	}

	public StyledString getStyledText(Object element) {
		final StagingEntry c = (StagingEntry) element;
		final DecorationResult decoration = new DecorationResult();
		decorationHelper.decorate(decoration, c);

		final StyledString styled = new StyledString();
		final String prefix = decoration.getPrefix();
		final String suffix = decoration.getSuffix();
		if (prefix != null)
			styled.append(prefix, StyledString.DECORATIONS_STYLER);
		if (fileNameMode) {
			IPath parsed = Path.fromOSString(c.getPath());
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
				styled.append(c.getPath());
				if (suffix != null)
					styled.append(suffix, StyledString.DECORATIONS_STYLER);
			}
		} else {
			styled.append(c.getPath());
			if (suffix != null)
				styled.append(suffix, StyledString.DECORATIONS_STYLER);
		}

		return styled;
	}

	public Image getImage(Object element) {
		final StagingEntry c = (StagingEntry) element;
		final DecorationResult decoration = new DecorationResult();
		decorationHelper.decorate(decoration, c);
		return getDecoratedImage(getEditorImage(c), decoration.getOverlay());
	}
}
