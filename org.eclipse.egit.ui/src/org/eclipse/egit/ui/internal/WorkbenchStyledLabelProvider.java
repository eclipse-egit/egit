/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Styled label provider that wraps a {@link WorkbenchLabelProvider}
 */
public class WorkbenchStyledLabelProvider implements IStyledLabelProvider {

	/**
	 * Workbench label provider
	 */
	protected final WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();

	public void removeListener(ILabelProviderListener listener) {
		// Empty
	}

	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	public void dispose() {
		workbenchLabelProvider.dispose();
	}

	public void addListener(ILabelProviderListener listener) {
		// Empty
	}

	public StyledString getStyledText(Object element) {
		return new StyledString(workbenchLabelProvider.getText(element));
	}

	public Image getImage(Object element) {
		return workbenchLabelProvider.getImage(element);
	}
}
