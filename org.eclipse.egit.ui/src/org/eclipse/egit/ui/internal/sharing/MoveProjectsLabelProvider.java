/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.sharing;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * A label provide for showing projects to be moved during sharing
 */
public class MoveProjectsLabelProvider extends BaseLabelProvider implements
		ITableLabelProvider {
	IPath targetFolder;
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		IProject prj = (IProject) element;
		switch (columnIndex) {
		case 0:
			return prj.getName();
		case 1:
			return prj.getLocation().toString();
		case 2:
			if (targetFolder != null)
				return targetFolder.append(prj.getName()).toString();
			return null;
		default:
			return null;
		}
	}
}
