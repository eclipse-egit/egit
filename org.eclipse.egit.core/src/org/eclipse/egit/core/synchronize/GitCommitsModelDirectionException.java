/*******************************************************************************
 * Copyright (C) 2012, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

/**
 * Thrown when cannot determinate commit direction during
 * {@link GitCommitsModelCache#build(org.eclipse.jgit.lib.Repository, org.eclipse.jgit.lib.ObjectId, org.eclipse.jgit.lib.ObjectId)}
 *
 */
public class GitCommitsModelDirectionException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = 7867729888561453855L;

	/**
	 * Creates exception instance with default message
	 */
	public GitCommitsModelDirectionException() {
		super("Unknown commit direction"); //$NON-NLS-1$
	}

}
