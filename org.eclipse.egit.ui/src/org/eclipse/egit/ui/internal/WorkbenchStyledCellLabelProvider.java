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

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Simple extensions of {@link DelegatingStyledCellLabelProvider} which uses a
 * {@link WorkbenchLabelProvider} for use with objects that adapt to
 * {@link IWorkbenchAdapter}.
 */
public class WorkbenchStyledCellLabelProvider extends
		DelegatingStyledCellLabelProvider implements ILabelProvider {

	private final WorkbenchLabelProvider wrapped;

	/**
	 * Create workbenck styled cell label provider
	 */
	public WorkbenchStyledCellLabelProvider() {
		super(new WorkbenchLabelProvider());
		wrapped = (WorkbenchLabelProvider) getStyledStringProvider();
	}

	public String getText(Object element) {
		return wrapped.getText(element);
	}
}