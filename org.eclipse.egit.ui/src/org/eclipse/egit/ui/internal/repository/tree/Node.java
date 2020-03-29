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

import org.eclipse.jgit.lib.Repository;

/**
 * Common basic interface for repository tree nodes.
 */
public interface Node {

	/**
	 * Retrieves the node's {@link Repository}.
	 *
	 * @return the repository
	 */
	Repository getRepository();

	/**
	 * Retrieves the node's type.
	 *
	 * @return the {@link RepositoryTreeNodeType}
	 */
	RepositoryTreeNodeType getType();
}
