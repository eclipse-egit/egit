/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

/**
 * An abstraction for an object from a repository.
 */
public interface IRepositoryObject {
	// TODO: extend IAdaptable once EGit baseline is Eclipse 4.5 (Mars), provide
	// adaptation to Repository and to ? super ObjectId. Right now we cannot do
	// so because for binary compatibility we need to use non-generic
	// getAdapter(), which then cannot be invoked via
	// IRepositoryObject.super.getAdapter().

	/**
	 * Retrieves the {@link Repository} the commit was loaded from.
	 *
	 * @return the {@link Repository}
	 */
	Repository getRepository();

	/**
	 * Retrieves the {@link ObjectId} of the object.
	 *
	 * @return the {@link ObjectId}
	 */
	ObjectId getObjectId();
}
