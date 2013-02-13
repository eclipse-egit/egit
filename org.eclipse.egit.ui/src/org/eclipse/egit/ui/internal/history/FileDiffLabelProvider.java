/*******************************************************************************
 * Copyright (C) 2008, 2013 Shawn O. Pearce <spearce@spearce.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

/**
 * Label provider for {@link FileDiff} objects
 */
public class FileDiffLabelProvider extends BaseLabelProvider implements
		ITableLabelProvider, ITableColorProvider {

	private final ResourceManager resourceManager = new LocalResourceManager(
			JFaceResources.getResources());
	private final Color dimmedForegroundColor;

	/**
	 * @param dimmedForegroundRgb the color used for as foreground color for "unhighlighted" entries
	 */
	public FileDiffLabelProvider(RGB dimmedForegroundRgb) {
		dimmedForegroundColor = resourceManager.createColor(dimmedForegroundRgb);
	}

	public String getColumnText(final Object element, final int columnIndex) {
		if (columnIndex == 0)
			return ((FileDiff) element).getLabel(element);
		return null;
	}

	public Image getColumnImage(final Object element, final int columnIndex) {
		if (columnIndex == 0) {
			final FileDiff c = (FileDiff) element;
			return (Image) resourceManager.get(c.getImageDescriptor(c));
		}
		return null;
	}

	@Override
	public void dispose() {
		this.resourceManager.dispose();
		super.dispose();
	}

	public Color getForeground(Object element, int columnIndex) {
		if (columnIndex == 0) {
			final FileDiff c = (FileDiff) element;
			if (!c.isMarked(FileDiffContentProvider.INTERESTING_MARK_TREE_FILTER_INDEX))
				return dimmedForegroundColor;
		}
		return null;
	}

	public Color getBackground(Object element, int columnIndex) {
		// Use default color
		return null;
	}
}
