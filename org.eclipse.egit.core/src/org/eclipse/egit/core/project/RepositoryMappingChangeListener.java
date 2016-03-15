/*******************************************************************************
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.project;

import org.eclipse.jgit.annotations.NonNull;

/**
 * Receives notification when a new {@link RepositoryMapping} appears in the
 * Eclipse resource tree.
 * <p>
 * A change listener may be called from any thread, especially background job
 * threads, but also from the UI thread. Implementors are encouraged to complete
 * quickly, and make arrange for their tasks to run on the UI event thread if
 * necessary.
 * </p>
 */
public interface RepositoryMappingChangeListener {
	/**
	 * Invoked when a new {@link RepositoryMapping} appears in the Eclipse
	 * resource tree.
	 *
	 * @param which
	 *            the new {@link RepositoryMapping}
	 */
	public void repositoryChanged(@NonNull RepositoryMapping which);
}
