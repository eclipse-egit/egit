/*******************************************************************************
 * Copyright (c) 2013 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;

/**
 * Wraps {@link RepositoriesViewLabelProvider} in a
 * {@link DelegatingStyledCellLabelProvider} to provide styled text support for
 * use in tree or table viewers.
 * <p>
 * Also provides support for tool tips (see bug 236006 in platform which would
 * make this unnecessary).
 * <p>
 * Also implements ILabelProvider for use with PatternFilter (see bug 258029 in
 * platform which would make this unnecessary).
 */
public class RepositoriesViewStyledCellLabelProvider extends
		DelegatingStyledCellLabelProvider implements ILabelProvider {

	/** */
	public RepositoriesViewStyledCellLabelProvider() {
		super(new RepositoriesViewLabelProvider());
	}

	@Override
	public String getText(Object element) {
		return getStyledStringProvider().getStyledText(element).getString();
	}

	@Override
	public String getToolTipText(Object element) {
		return ((RepositoriesViewLabelProvider) getStyledStringProvider())
				.getToolTipText(element);
	}

}
