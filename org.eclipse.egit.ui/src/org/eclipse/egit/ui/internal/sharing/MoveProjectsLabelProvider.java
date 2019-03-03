/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.sharing;

import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;

/**
 * A label provider for showing projects to be moved during sharing
 */
public class MoveProjectsLabelProvider extends BaseLabelProvider implements
		ITableLabelProvider {
	IPath targetFolder;

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0)
			return PlatformUI.getWorkbench().getSharedImages()
					.getImage(SharedImages.IMG_OBJ_PROJECT);
		else
			return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		IProject prj = (IProject) element;
		switch (columnIndex) {
		case 0:
			return prj.getName();
		case 1:
			IPath location = prj.getLocation();
			if (location != null) {
				return location.toString();
			}
			URI locationURI = prj.getLocationURI();
			if (locationURI != null) {
				return locationURI.toString();
			}
			return null;
		case 2:
			if (targetFolder != null)
				return targetFolder.append(prj.getName()).toString();
			return null;
		default:
			return null;
		}
	}
}
