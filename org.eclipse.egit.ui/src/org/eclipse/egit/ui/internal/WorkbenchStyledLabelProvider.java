/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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

	@Override
	public void removeListener(ILabelProviderListener listener) {
		// Empty
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void dispose() {
		workbenchLabelProvider.dispose();
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		// Empty
	}

	@Override
	public StyledString getStyledText(Object element) {
		return new StyledString(workbenchLabelProvider.getText(element));
	}

	@Override
	public Image getImage(Object element) {
		return workbenchLabelProvider.getImage(element);
	}
}
