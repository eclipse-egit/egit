/*******************************************************************************
 * Copyright (C) 2020, Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.egit.ui.internal.repository.tree.command.FilterCommand;

/**
 * A {@link FilterableNode} supports filtering its children.
 *
 * @see FilterCommand
 */
public interface FilterableNode extends Node {

	/**
	 * Retrieves the current filter pattern.
	 *
	 * @return the filter pattern; may be {@code null}
	 */
	String getFilter();

	/**
	 * Sets the current filter pattern.
	 *
	 * @param filter
	 *            to set
	 */
	void setFilter(String filter);
}
