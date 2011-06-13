/*******************************************************************************
 * Copyright (c) 2011, Chris Aniszczyk <caniszczyk@gmail.com> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Chris Aniszczyk <caniszczyk@gmail.com> - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.reflog;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jgit.storage.file.ReflogEntry;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for {@link ReflogEntry} objects
 */
public class ReflogLabelProvider extends BaseLabelProvider implements
		ITableLabelProvider {

	private ResourceManager resourceManager = new LocalResourceManager(
			JFaceResources.getResources());

	public Image getColumnImage(Object element, int columnIndex) {
		final ReflogEntry entry = (ReflogEntry) element;
		if (columnIndex == 0) {
			// TODO figure this out
		}
		System.out.println(entry);
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		final ReflogEntry entry = (ReflogEntry) element;
		if (columnIndex == 0) {
			return entry.getOldId().toString();
		} else if (columnIndex == 1) {
			return entry.getNewId().toString();
		} else if (columnIndex == 2) {
			return entry.getComment();
		}
		return null;
	}

	@Override
	public void dispose() {
		this.resourceManager.dispose();
		super.dispose();
	}
}
